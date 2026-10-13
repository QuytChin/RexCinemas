package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock ShowtimeSeatRepository seats;
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock ShowtimeRepository showtimes;
    @Mock SeatBroadcastService broadcast;
    @Mock ComboProductRepository combos;
    @Mock VoucherRepository vouchers;
    @Mock SeatReservationService reservations;
    @Mock PaymentService payment;
    @Mock CheckInService checkIn;
    @Mock SePayPaymentService sepay;

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(seats, bookings, users, showtimes, broadcast, combos, vouchers,
                reservations, payment, checkIn, sepay);
    }

    @Test
    void hiddenCinemaRejectsBookingBeforeSeatMutation() {
        User user = new User();
        user.setId(7L);
        user.setRole(Role.CUSTOMER);
        Showtime showtime = futureShowtime(false);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(showtimes.findById(11L)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> service.confirm(7L, 11L, List.of(21L), PaymentMethod.MOMO_SANDBOX,
                null, List.of(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Suất chiếu");

        verify(seats, never()).findByIdInForUpdate(anyList());
        verify(bookings, never()).save(any(Booking.class));
    }

    private Showtime futureShowtime(boolean cinemaActive) {
        Cinema cinema = new Cinema();
        cinema.setName("Rex Test");
        cinema.setCity("TP.HCM");
        cinema.setAddress("Test address");
        cinema.setActive(cinemaActive);
        Auditorium auditorium = new Auditorium();
        auditorium.setName("Phòng 01");
        auditorium.setCinema(cinema);
        Movie movie = new Movie();
        movie.setTitle("Phim test");
        movie.setDurationMinutes(100);
        Showtime showtime = new Showtime();
        showtime.setId(11L);
        showtime.setActive(true);
        showtime.setStartTime(LocalDateTime.now().plusHours(2));
        showtime.setAuditorium(auditorium);
        showtime.setMovie(movie);
        return showtime;
    }
}
