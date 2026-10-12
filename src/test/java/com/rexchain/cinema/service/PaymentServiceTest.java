package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock BookingRepository bookings;
    @Mock ShowtimeSeatRepository seats;
    @Mock UserRepository users;
    @Mock ComboProductRepository combos;
    @Mock VoucherRepository vouchers;
    @Mock SeatBroadcastService broadcast;
    @Mock SeatReservationService reservations;
    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(bookings, seats, users, combos, vouchers, broadcast, reservations);
    }

    @Test
    void successfulCallbackBooksHeldSeatAndAwardsPointsExactlyOnce() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = new User();
        user.setId(7L);
        Showtime showtime = new Showtime();
        showtime.setId(11L);
        ShowtimeSeat seat = new ShowtimeSeat();
        seat.setId(21L);
        seat.setStatus(SeatStatus.HELD);
        seat.setHeldByUserId(7L);
        seat.setHoldUntil(LocalDateTime.now().plusMinutes(5));
        BookingSeat line = new BookingSeat();
        line.setShowtimeSeat(seat);
        Booking booking = pendingBooking(user, showtime, line);
        ComboProduct combo = new ComboProduct();
        combo.setId(31L);
        combo.setName("Combo test");
        combo.setActive(true);
        combo.setStockQuantity(5);
        BookingCombo comboLine = new BookingCombo();
        comboLine.setBooking(booking);
        comboLine.setCombo(combo);
        comboLine.setQuantity(2);
        booking.getCombos().add(comboLine);
        Voucher voucher = new Voucher();
        voucher.setId(41L);
        voucher.setCode("TEST10");
        voucher.setActive(true);
        voucher.setUsageLimit(10);
        voucher.setUsedCount(3);
        booking.setVoucherCode(voucher.getCode());

        when(bookings.findByBookingCodeForUpdate("REX1")).thenReturn(Optional.of(booking));
        when(seats.findByIdInForUpdate(List.of(21L))).thenReturn(List.of(seat));
        when(combos.findByIdInForUpdate(List.of(31L))).thenReturn(List.of(combo));
        when(vouchers.findByCodeIgnoreCaseForUpdate("TEST10")).thenReturn(Optional.of(voucher));
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        Booking result = service.completeOnlinePayment("REX1", "TX-100");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getPaymentReference()).isEqualTo("TX-100");
        assertThat(result.isPointsAwarded()).isTrue();
        assertThat(user.getLoyaltyPoints()).isEqualTo(2);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(combo.getStockQuantity()).isEqualTo(3);
        assertThat(voucher.getUsedCount()).isEqualTo(4);
        verify(users, times(1)).save(user);
        verify(combos, times(1)).saveAll(List.of(combo));
        verify(vouchers, times(1)).save(voucher);
        verify(broadcast).send(11L, List.of(21L), "BOOKED");

        service.completeOnlinePayment("REX1", "TX-100");
        assertThat(combo.getStockQuantity()).isEqualTo(3);
        assertThat(voucher.getUsedCount()).isEqualTo(4);
        verify(users, times(1)).save(user);
        verify(combos, times(1)).saveAll(List.of(combo));
        verify(vouchers, times(1)).save(voucher);
        verify(broadcast, times(1)).send(anyLong(), anyList(), anyString());
    }

    @Test
    void callbackWithoutGatewayReferenceIsRejectedBeforeAnySideEffect() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.completeOnlinePayment("REX1", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã giao dịch");

        verifyNoInteractions(bookings, seats, users, combos, vouchers, broadcast, reservations);
    }

    @Test
    void lateSuccessfulCallbackMovesExpiredBookingToRefundQueue() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = new User();
        user.setId(7L);
        Booking booking = new Booking();
        booking.setBookingCode("REX-LATE");
        booking.setUser(user);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setPaymentMethod(PaymentMethod.MOMO_SANDBOX);
        booking.setTotalAmount(BigDecimal.valueOf(100_000));
        when(bookings.findByBookingCodeForUpdate("REX-LATE")).thenReturn(Optional.of(booking));

        Booking result = service.completeOnlinePayment("REX-LATE", "MOMO-9");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REFUND_PENDING);
        assertThat(result.getPaidAt()).isNotNull();
        verify(reservations).releaseBookingSeats(booking);
        verifyNoInteractions(users, combos, vouchers, broadcast);
    }

    private Booking pendingBooking(User user, Showtime showtime, BookingSeat line) {
        Booking booking = new Booking();
        booking.setBookingCode("REX1");
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentMethod(PaymentMethod.MOMO_SANDBOX);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        booking.setTotalAmount(BigDecimal.valueOf(25_000));
        line.setBooking(booking);
        booking.getSeats().add(line);
        return booking;
    }
}
