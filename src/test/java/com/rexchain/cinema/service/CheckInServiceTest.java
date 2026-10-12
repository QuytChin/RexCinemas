package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.CheckInResult;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.BookingSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {
    @Mock BookingRepository bookings;
    @Mock BookingSeatRepository bookingSeats;

    @Test
    void repeatedGroupScanIsRejectedAtomically() {
        Booking booking = confirmedBooking();
        when(bookings.findByBookingCode("REX-GROUP")).thenReturn(Optional.of(booking));
        when(bookings.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(bookings.atomicCheckIn(eq(30L), any(LocalDateTime.class), eq("gate@rex.vn"))).thenReturn(0);

        CheckInResult result = new CheckInService(bookings, bookingSeats)
                .checkInTicket("REX-GROUP", "gate@rex.vn");

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("ALREADY_CHECKED_IN");
        verify(bookingSeats, never()).atomicCheckInAllSeats(anyLong(), any(), anyString());
    }

    private Booking confirmedBooking() {
        Movie movie = new Movie();
        movie.setDurationMinutes(120);
        Showtime showtime = new Showtime();
        showtime.setStartTime(LocalDateTime.now().plusMinutes(10));
        showtime.setMovie(movie);
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setBookingCode("REX-GROUP");
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }
}
