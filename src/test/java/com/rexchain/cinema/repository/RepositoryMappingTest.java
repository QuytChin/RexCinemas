package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Auditorium;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class RepositoryMappingTest {
    @Autowired ShowtimeSeatRepository showtimeSeats;
    @Autowired BookingRepository bookings;
    @Autowired CinemaRepository cinemas;
    @Autowired AuditoriumRepository auditoriums;
    @Autowired MovieRepository movies;
    @Autowired ShowtimeRepository showtimes;
    @Autowired UserRepository users;

    @Test
    void customQueriesAreParsedAndExecutable() {
        assertThat(showtimeSeats.summarizeAvailability(List.of(-1L))).isEmpty();
        assertThat(bookings.findByBookingCodeForUpdate("missing")).isEmpty();
    }

    @Test
    void publicShowtimeQueryExcludesHiddenCinemaAndRestoresAfterReactivation() {
        Cinema active = cinema("Rex Active", true);
        Cinema hidden = cinema("Rex Hidden", false);
        cinemas.saveAllAndFlush(List.of(active, hidden));

        Auditorium activeRoom = auditorium(active, "Phòng 01");
        Auditorium hiddenRoom = auditorium(hidden, "Phòng 01");
        auditoriums.saveAllAndFlush(List.of(activeRoom, hiddenRoom));

        Movie movie = new Movie();
        movie.setTitle("Phim kiểm thử visibility");
        movie.setDescription("Nội dung kiểm thử");
        movie.setDurationMinutes(100);
        movie.setReleaseDate(LocalDate.now().minusDays(1));
        movies.saveAndFlush(movie);

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        Showtime activeShowtime = showtime(movie, activeRoom, start);
        Showtime hiddenShowtime = showtime(movie, hiddenRoom, start.plusMinutes(15));
        showtimes.saveAllAndFlush(List.of(activeShowtime, hiddenShowtime));

        assertThat(showtimes.findPublicByMovieIdAndStartTimeAfterOrderByStartTimeAsc(movie.getId(), LocalDateTime.now()))
                .extracting(s -> s.getAuditorium().getCinema().getName())
                .containsExactly("Rex Active");

        hidden.setActive(true);
        cinemas.saveAndFlush(hidden);

        assertThat(showtimes.findPublicByMovieIdAndStartTimeAfterOrderByStartTimeAsc(movie.getId(), LocalDateTime.now()))
                .extracting(s -> s.getAuditorium().getCinema().getName())
                .containsExactly("Rex Active", "Rex Hidden");

        assertThat(cinemas.findByActiveTrueOrderByCityAscNameAsc())
                .extracting(Cinema::getName)
                .containsExactly("Rex Active", "Rex Hidden");
        assertThat(cinemas.findAll())
                .extracting(Cinema::getName)
                .contains("Rex Hidden");
    }

    @Test
    void confirmedBookingHistoryRemainsAvailableAfterCinemaIsHidden() {
        Cinema cinema = cinema("Rex History", true);
        cinemas.saveAndFlush(cinema);
        Auditorium auditorium = auditorium(cinema, "Phòng 02");
        auditoriums.saveAndFlush(auditorium);

        Movie movie = new Movie();
        movie.setTitle("Phim lịch sử");
        movie.setDescription("Nội dung kiểm thử");
        movie.setDurationMinutes(100);
        movie.setReleaseDate(LocalDate.now().minusDays(1));
        movies.saveAndFlush(movie);

        Showtime showtime = showtime(movie, auditorium, LocalDateTime.now().minusHours(2));
        showtimes.saveAndFlush(showtime);

        User user = new User();
        user.setFullName("Khách hàng lịch sử");
        user.setEmail("history@example.com");
        user.setPassword("test-password");
        users.saveAndFlush(user);

        Booking booking = new Booking();
        booking.setBookingCode("REXHISTORY01");
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setTotalAmount(new BigDecimal("80000"));
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookings.saveAndFlush(booking);

        cinema.setActive(false);
        cinemas.saveAndFlush(cinema);

        assertThat(bookings.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .extracting(Booking::getBookingCode)
                .containsExactly("REXHISTORY01");
    }

    private Cinema cinema(String name, boolean active) {
        Cinema cinema = new Cinema();
        cinema.setName(name);
        cinema.setCity("TP.HCM");
        cinema.setAddress(name + " address");
        cinema.setActive(active);
        return cinema;
    }

    private Auditorium auditorium(Cinema cinema, String name) {
        Auditorium auditorium = new Auditorium();
        auditorium.setCinema(cinema);
        auditorium.setName(name);
        auditorium.setTotalRows(5);
        auditorium.setSeatsPerRow(5);
        return auditorium;
    }

    private Showtime showtime(Movie movie, Auditorium auditorium, LocalDateTime start) {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(start);
        showtime.setBasePrice(new BigDecimal("80000"));
        return showtime;
    }
}
