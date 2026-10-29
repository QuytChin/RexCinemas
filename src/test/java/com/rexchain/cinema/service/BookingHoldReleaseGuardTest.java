package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.ComboProductRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import com.rexchain.cinema.repository.ShowtimeSeatRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The controller cleanup path runs after a failed checkout request. When the booking
 * itself was already committed, its seats must stay held for that pending booking.
 */
@ExtendWith(MockitoExtension.class)
class BookingHoldReleaseGuardTest {
    @Mock ShowtimeSeatRepository seats;
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock ShowtimeRepository showtimes;
    @Mock SeatBroadcastService broadcast;
    @Mock ComboProductRepository combos;
    @Mock VoucherRepository vouchers;
    @Mock SeatReservationService reservations;
    @Mock PaymentService payments;
    @Mock CheckInService checkIns;
    @Mock SePayPaymentService sepay;
    BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(seats, bookings, users, showtimes, broadcast, combos, vouchers,
                reservations, payments, checkIns, sepay);
    }

    @Test
    void seatsStayHeldWhenAPendingBookingAlreadyOwnsThem() {
        when(bookings.existsByUserIdAndShowtimeIdAndStatusInAndExpiresAtAfter(
                eq(7L), eq(11L), eq(List.of(BookingStatus.PENDING)), any(LocalDateTime.class)))
                .thenReturn(true);

        service.releaseUnconfirmedHolds(7L, 11L, List.of(21L, 22L));

        verifyNoInteractions(reservations);
    }

    @Test
    void seatsAreReleasedWhenNoBookingWasCreated() {
        when(bookings.existsByUserIdAndShowtimeIdAndStatusInAndExpiresAtAfter(
                eq(7L), eq(11L), eq(List.of(BookingStatus.PENDING)), any(LocalDateTime.class)))
                .thenReturn(false);

        service.releaseUnconfirmedHolds(7L, 11L, List.of(21L, 22L));

        verify(reservations).releaseOwnHolds(7L, 11L, List.of(21L, 22L));
    }

    @Test
    void explicitCustomerReleaseIsNeverBlockedByTheGuard() {
        service.releaseOwnHolds(7L, 11L, List.of(21L));

        verify(reservations).releaseOwnHolds(7L, 11L, List.of(21L));
        verify(bookings, never()).existsByUserIdAndShowtimeIdAndStatusInAndExpiresAtAfter(
                anyLong(), anyLong(), anyList(), any(LocalDateTime.class));
    }
}
