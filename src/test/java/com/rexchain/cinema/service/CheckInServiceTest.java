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
import java.util.List;
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

    @Test
    void confirmedGroupBookingChecksInAllSeats() {
        Booking booking = confirmedBooking();
        BookingSeat first = seat(booking, "A1", "TKT-REX-GROUP-A1");
        BookingSeat second = seat(booking, "A2", "TKT-REX-GROUP-A2");
        booking.setSeats(List.of(first, second));
        when(bookings.findByBookingCode("REX-GROUP")).thenReturn(Optional.of(booking));
        when(bookings.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));
        when(bookings.atomicCheckIn(eq(30L), any(), eq("gate@rex.vn"))).thenReturn(1);
        when(bookingSeats.atomicCheckInAllSeats(eq(30L), any(), eq("gate@rex.vn"))).thenReturn(2);

        CheckInResult result = new CheckInService(bookings, bookingSeats)
                .checkInTicket("REX-GROUP", "gate@rex.vn");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("CHECKED_IN");
        assertThat(first.isCheckedIn()).isTrue();
        assertThat(second.isCheckedIn()).isTrue();
        verify(bookingSeats).atomicCheckInAllSeats(eq(30L), any(), eq("gate@rex.vn"));
    }

    @Test
    void nonConfirmedBookingCannotCheckIn() {
        Booking booking = confirmedBooking();
        booking.setStatus(BookingStatus.PENDING);
        when(bookings.findByBookingCode("REX-GROUP")).thenReturn(Optional.of(booking));
        when(bookings.findByIdForUpdate(30L)).thenReturn(Optional.of(booking));

        CheckInResult result = new CheckInService(bookings, bookingSeats)
                .checkInTicket("REX-GROUP", "gate@rex.vn");

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("PENDING");
        verify(bookings, never()).atomicCheckIn(anyLong(), any(), anyString());
        verify(bookingSeats, never()).atomicCheckInAllSeats(anyLong(), any(), anyString());
    }

    private BookingSeat seat(Booking booking, String label, String ticketCode) {
        BookingSeat seat = new BookingSeat();
        seat.setBooking(booking);
        seat.setSeatLabel(label);
        seat.setTicketCode(ticketCode);
        return seat;
    }

    private Booking confirmedBooking() {
        Movie movie = new Movie();
        movie.setDurationMinutes(120);
        Showtime showtime = new Showtime();
        showtime.setStartTime(LocalDateTime.now().minusMinutes(10));
        showtime.setMovie(movie);
        Cinema cinema = new Cinema();
        cinema.setName("Rex Chain Quận 1");
        Auditorium auditorium = new Auditorium();
        auditorium.setName("Phòng 01");
        auditorium.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        User user = new User();
        user.setFullName("Khách kiểm thử");
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setBookingCode("REX-GROUP");
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }
}
