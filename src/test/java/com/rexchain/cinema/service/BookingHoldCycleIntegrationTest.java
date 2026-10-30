package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Uses only the isolated in-memory test database; no gateway or real payment is called. */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"})
class BookingHoldCycleIntegrationTest {
    @Autowired CinemaRepository cinemas;
    @Autowired AuditoriumRepository auditoriums;
    @Autowired MovieRepository movies;
    @Autowired ShowtimeRepository showtimes;
    @Autowired SeatRepository physicalSeats;
    @Autowired ShowtimeSeatRepository showtimeSeats;
    @Autowired SeatHoldSessionRepository holdSessions;
    @Autowired BookingSeatRepository bookingSeats;
    @Autowired BookingRepository bookings;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void paidBookingAThenNewBAndCEachReceiveAFullIndependentHold() {
        Fixture fixture = fixture();
        SeatBroadcastService broadcast = mock(SeatBroadcastService.class);
        SeatReservationService holds = new SeatReservationService(showtimeSeats, showtimes, bookingSeats,
                broadcast, holdSessions, bookings, users);
        PaymentService payments = new PaymentService(bookings, showtimeSeats, users,
                mock(ComboProductRepository.class), mock(VoucherRepository.class), broadcast, holds);

        LocalDateTime a = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 0));
        assertThat(a).isBetween(LocalDateTime.now().plusMinutes(4).plusSeconds(50),
                LocalDateTime.now().plusMinutes(5).plusSeconds(10));
        // Reproduce the reported 01:30 by advancing only the old persisted deadline.
        LocalDateTime aRemaining = LocalDateTime.now().plusSeconds(90);
        shortenCurrentHold(fixture, pair(fixture, 0), aRemaining);
        Booking first = pending(fixture, pair(fixture, 0), aRemaining, "REX_TEST_A");
        entityManager.clear(); // Payment webhook is a separate request and reloads every row.
        assertThat(payments.completeOnlinePayment(first.getBookingCode(), "REX_TEST_TX_A", LocalDateTime.now())
                .getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(holdSessions.findByUserIdAndShowtimeIdForUpdate(fixture.user.getId(), fixture.showtime.getId()))
                .isEmpty();

        LocalDateTime b = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 2));
        assertThat(b).isAfter(aRemaining.plusMinutes(3));
        assertThat(b).isBetween(LocalDateTime.now().plusMinutes(4).plusSeconds(50),
                LocalDateTime.now().plusMinutes(5).plusSeconds(10));
        // A repeated hold in the same checkout must not extend B.
        assertThat(holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 2))).isEqualTo(b);
        LocalDateTime bRemaining = LocalDateTime.now().plusSeconds(30);
        shortenCurrentHold(fixture, pair(fixture, 2), bRemaining);
        Booking second = pending(fixture, pair(fixture, 2), bRemaining, "REX_TEST_B");
        entityManager.clear();
        assertThat(payments.completeOnlinePayment(second.getBookingCode(), "REX_TEST_TX_B", LocalDateTime.now())
                .getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        LocalDateTime c = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 4));
        assertThat(c).isAfter(bRemaining.plusMinutes(4));
        assertThat(c).isBetween(LocalDateTime.now().plusMinutes(4).plusSeconds(50),
                LocalDateTime.now().plusMinutes(5).plusSeconds(10));
        assertThat(bookings.findByBookingCode(first.getBookingCode()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void oldExpiredBookingCannotReleaseOrSettleASeatReheldInAnotherCycle() {
        Fixture fixture = fixture();
        SeatBroadcastService broadcast = mock(SeatBroadcastService.class);
        SeatReservationService holds = new SeatReservationService(showtimeSeats, showtimes, bookingSeats,
                broadcast, holdSessions, bookings, users);
        PaymentService payments = new PaymentService(bookings, showtimeSeats, users,
                mock(ComboProductRepository.class), mock(VoucherRepository.class), broadcast, holds);
        List<Long> ids = pair(fixture, 0);
        holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), ids);
        LocalDateTime oldDeadline = LocalDateTime.now().minusSeconds(2);
        shortenCurrentHold(fixture, ids, oldDeadline);
        Booking old = pending(fixture, ids, oldDeadline, "REX_TEST_OLD");
        entityManager.clear();

        LocalDateTime newDeadline = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), ids);
        assertThat(newDeadline).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(holds.isHoldActive(fixture.user.getId(), fixture.showtime.getId(), ids, epoch(oldDeadline))).isFalse();
        assertThat(holds.isHoldActive(fixture.user.getId(), fixture.showtime.getId(), ids, epoch(newDeadline))).isTrue();

        assertThat(payments.completeOnlinePayment(old.getBookingCode(), "REX_TEST_LATE", oldDeadline.minusSeconds(1))
                .getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(holds.isHoldActive(fixture.user.getId(), fixture.showtime.getId(), ids, epoch(newDeadline))).isTrue();
    }

    @Test
    void existingSessionLeftByAnAlreadyCompletedBookingIsReplaced() {
        Fixture fixture = fixture();
        SeatReservationService holds = new SeatReservationService(showtimeSeats, showtimes, bookingSeats,
                mock(SeatBroadcastService.class), holdSessions, bookings, users);
        LocalDateTime firstDeadline = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 0));
        Long firstSessionId = holdSessions.findByUserIdAndShowtimeIdForUpdate(
                fixture.user.getId(), fixture.showtime.getId()).orElseThrow().getId();
        Booking alreadyPaid = pending(fixture, pair(fixture, 0), firstDeadline, "REX_TEST_LEGACY");
        alreadyPaid.setStatus(BookingStatus.CONFIRMED);
        alreadyPaid.setExpiresAt(null);
        bookings.saveAndFlush(alreadyPaid);
        for (ShowtimeSeat seat : showtimeSeats.findByIdInForUpdate(pair(fixture, 0))) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHoldUntil(null);
            seat.setHeldByUserId(null);
        }
        showtimeSeats.flush();

        LocalDateTime secondDeadline = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 2));
        assertThat(secondDeadline).isAfter(firstDeadline);
        assertThat(holdSessions.findByUserIdAndShowtimeIdForUpdate(
                fixture.user.getId(), fixture.showtime.getId()).orElseThrow().getId()).isNotEqualTo(firstSessionId);
    }

    @Test
    void anotherHoldCannotExtendAStillPendingBooking() {
        Fixture fixture = fixture();
        SeatReservationService holds = new SeatReservationService(showtimeSeats, showtimes, bookingSeats,
                mock(SeatBroadcastService.class), holdSessions, bookings, users);
        LocalDateTime deadline = holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 0));
        pending(fixture, pair(fixture, 0), deadline, "REX_TEST_PENDING");

        assertThatThrownBy(() -> holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), pair(fixture, 2)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("chờ thanh toán");
        assertThat(holdSessions.findByUserIdAndShowtimeIdForUpdate(
                fixture.user.getId(), fixture.showtime.getId()).orElseThrow().getExpiresAt()).isEqualTo(deadline);
    }

    @Test
    void expiredBookingReleasesItsOwnSeatsAndASecondCustomerCannotStealAnActiveHold() {
        Fixture fixture = fixture();
        SeatBroadcastService broadcast = mock(SeatBroadcastService.class);
        SeatReservationService holds = new SeatReservationService(showtimeSeats, showtimes, bookingSeats,
                broadcast, holdSessions, bookings, users);
        PaymentService payments = new PaymentService(bookings, showtimeSeats, users,
                mock(ComboProductRepository.class), mock(VoucherRepository.class), broadcast, holds);
        List<Long> ids = pair(fixture, 0);
        holds.holdSeats(fixture.user.getId(), fixture.showtime.getId(), ids);
        User other = new User();
        other.setEmail("other-hold@example.invalid"); other.setFullName("REX_TEST_OTHER");
        other.setPassword("test-only-placeholder"); users.saveAndFlush(other);
        assertThatThrownBy(() -> holds.holdSeats(other.getId(), fixture.showtime.getId(), ids))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("người khác");
        assertThat(showtimeSeats.findById(ids.get(0)).orElseThrow().getHeldByUserId())
                .isEqualTo(fixture.user.getId());

        LocalDateTime expiredDeadline = LocalDateTime.now().minusSeconds(1);
        shortenCurrentHold(fixture, ids, expiredDeadline);
        Booking expired = pending(fixture, ids, expiredDeadline, "REX_TEST_EXPIRED");
        entityManager.clear();
        assertThat(payments.expireIfDue(expired.getBookingCode()).getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(showtimeSeats.findById(ids.get(0)).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(holdSessions.findByUserIdAndShowtimeIdForUpdate(
                fixture.user.getId(), fixture.showtime.getId())).isEmpty();
    }

    private List<Long> pair(Fixture fixture, int start) {
        return List.of(fixture.seats.get(start).getId(), fixture.seats.get(start + 1).getId());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.NOT_SUPPORTED)
    void parallelFirstHoldsBySameUserShareOneCheckoutSession() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Fixture fixture = transaction.execute(status -> fixture(suffix));
        SeatReservationService holds = new SeatReservationService(showtimeSeats, showtimes, bookingSeats,
                mock(SeatBroadcastService.class), holdSessions, bookings, users);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<LocalDateTime> first = executor.submit(() -> concurrentHold(
                    transaction, holds, fixture, pair(fixture, 0), ready, start));
            Future<LocalDateTime> second = executor.submit(() -> concurrentHold(
                    transaction, holds, fixture, pair(fixture, 2), ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS).truncatedTo(ChronoUnit.MICROS))
                    .isEqualTo(second.get(15, TimeUnit.SECONDS).truncatedTo(ChronoUnit.MICROS));
        } finally {
            transaction.executeWithoutResult(status -> {
                jdbc.update("delete from seat_hold_sessions where user_id = ?", fixture.user().getId());
                jdbc.update("delete from showtime_seats where showtime_id = ?", fixture.showtime().getId());
                jdbc.update("delete from seats where auditorium_id = ?", fixture.showtime().getAuditorium().getId());
                jdbc.update("delete from showtimes where id = ?", fixture.showtime().getId());
                jdbc.update("delete from movies where id = ?", fixture.showtime().getMovie().getId());
                jdbc.update("delete from app_users where id = ?", fixture.user().getId());
                jdbc.update("delete from auditoriums where id = ?", fixture.showtime().getAuditorium().getId());
                jdbc.update("delete from cinemas where id = ?", fixture.showtime().getAuditorium().getCinema().getId());
            });
        }
    }

    private LocalDateTime concurrentHold(TransactionTemplate transaction, SeatReservationService holds,
                                         Fixture fixture, List<Long> seatIds, CountDownLatch ready,
                                         CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent hold start timed out");
        return transaction.execute(status -> holds.holdSeats(
                fixture.user().getId(), fixture.showtime().getId(), seatIds));
    }

    private long epoch(LocalDateTime value) {
        return value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void shortenCurrentHold(Fixture fixture, List<Long> ids, LocalDateTime deadline) {
        SeatHoldSession session = holdSessions.findByUserIdAndShowtimeIdForUpdate(
                fixture.user.getId(), fixture.showtime.getId()).orElseThrow();
        session.setExpiresAt(deadline);
        holdSessions.saveAndFlush(session);
        for (ShowtimeSeat seat : showtimeSeats.findByIdInForUpdate(ids)) seat.setHoldUntil(deadline);
        showtimeSeats.flush();
    }

    private Booking pending(Fixture fixture, List<Long> ids, LocalDateTime deadline, String code) {
        Booking booking = new Booking();
        booking.setBookingCode(code);
        booking.setUser(fixture.user);
        booking.setShowtime(fixture.showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setTotalAmount(BigDecimal.valueOf(100_000));
        booking.setExpiresAt(deadline);
        for (ShowtimeSeat seat : showtimeSeats.findByIdIn(ids)) {
            BookingSeat line = new BookingSeat();
            line.setBooking(booking);
            line.setShowtimeSeat(seat);
            line.setSeatLabel(seat.getSeat().getLabel());
            line.setPrice(seat.getPrice());
            booking.getSeats().add(line);
        }
        return bookings.saveAndFlush(booking);
    }

    private Fixture fixture() {
        return fixture("");
    }

    private Fixture fixture(String suffix) {
        Cinema cinema = new Cinema();
        cinema.setName("REX_TEST_HOLD_CINEMA" + suffix); cinema.setCity("Test"); cinema.setAddress("Test");
        cinemas.saveAndFlush(cinema);
        Auditorium room = new Auditorium();
        room.setName("REX_TEST_ROOM"); room.setCinema(cinema); room.setTotalRows(1); room.setSeatsPerRow(6);
        auditoriums.saveAndFlush(room);
        Movie movie = new Movie();
        movie.setTitle("REX_TEST_HOLD_MOVIE" + suffix); movie.setDescription("Test");
        movie.setDurationMinutes(100); movie.setReleaseDate(LocalDate.now());
        movies.saveAndFlush(movie);
        Showtime showtime = new Showtime();
        showtime.setAuditorium(room); showtime.setMovie(movie);
        showtime.setStartTime(LocalDateTime.now().plusHours(2));
        showtime.setBasePrice(BigDecimal.valueOf(50_000));
        showtimes.saveAndFlush(showtime);
        User user = new User();
        user.setEmail("hold-cycle" + suffix + "@example.invalid"); user.setFullName("REX_TEST_HOLD_USER");
        user.setPassword("test-only-placeholder");
        users.saveAndFlush(user);
        List<ShowtimeSeat> seats = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Seat physical = new Seat();
            physical.setAuditorium(room); physical.setSeatRow("A"); physical.setSeatNumber(i);
            physicalSeats.saveAndFlush(physical);
            ShowtimeSeat seat = new ShowtimeSeat();
            seat.setShowtime(showtime); seat.setSeat(physical);
            seat.setPrice(BigDecimal.valueOf(50_000));
            seats.add(showtimeSeats.saveAndFlush(seat));
        }
        return new Fixture(user, showtime, seats);
    }

    private record Fixture(User user, Showtime showtime, List<ShowtimeSeat> seats) {}
}
