package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.dto.ManualRefundConfirmation;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void lateSuccessfulCallbackKeepsExpiredBookingUnavailableForTicketing() {
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

        assertThat(result.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(result.getPaidAt()).isNull();
        verifyNoInteractions(reservations);
        verifyNoInteractions(users, combos, vouchers, broadcast);
    }

    @Test
    void expiredQrStatusRequestReleasesPendingResourcesExactlyOnce() {
        User user = new User(); user.setId(7L);
        Booking booking = new Booking();
        booking.setBookingCode("REX-DUE"); booking.setUser(user); booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(bookings.findByBookingCodeForUpdate("REX-DUE")).thenReturn(Optional.of(booking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = service.expireIfDue("REX-DUE");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(result.getExpiresAt()).isNotNull();
        verify(reservations).releaseBookingSeats(booking);
    }

    @Test
    void manualRefundRequiresEvidenceAndIsIdempotentAfterConfirmation() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User customer = new User();
        customer.setId(7L);
        Booking booking = new Booking();
        booking.setId(44L);
        booking.setUser(customer);
        booking.setBookingCode("REX_TEST_REFUND");
        booking.setStatus(BookingStatus.REFUND_PENDING);
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setTotalAmount(BigDecimal.valueOf(100_000));
        when(bookings.findByIdForUpdate(44L)).thenReturn(Optional.of(booking));

        Booking confirmed = service.processRefund(44L, 9L, "admin@example.invalid",
                new ManualRefundConfirmation("TEST-TRANSFER-001", BigDecimal.valueOf(100_000),
                        "Đối soát trên sao kê test", "TEST receipt reference"));

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.REFUNDED);
        assertThat(confirmed.getRefundedAt()).isNotNull();
        assertThat(confirmed.getRefundedByUserId()).isEqualTo(9L);
        assertThat(confirmed.getRefundReference()).isEqualTo("TEST-TRANSFER-001");
        assertThat(confirmed.getRefundAmount()).isEqualByComparingTo("100000");
        assertThat(confirmed.getRefundEvidence()).isEqualTo("TEST receipt reference");

        Booking repeated = service.processRefund(44L, 9L, "admin@example.invalid",
                new ManualRefundConfirmation("SHOULD-NOT-REPLACE", BigDecimal.valueOf(100_000), null, null));
        assertThat(repeated).isSameAs(confirmed);
        assertThat(repeated.getRefundReference()).isEqualTo("TEST-TRANSFER-001");
        verify(bookings, times(1)).save(booking);
    }

    @Test
    void manualRefundStaysPendingWhenReferenceOrAmountCannotBeReconciled() {
        Booking booking = new Booking();
        booking.setId(45L);
        booking.setStatus(BookingStatus.REFUND_PENDING);
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setTotalAmount(BigDecimal.valueOf(100_000));
        when(bookings.findByIdForUpdate(45L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.processRefund(45L, 9L, "admin@example.invalid",
                new ManualRefundConfirmation("", BigDecimal.valueOf(100_000), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mã tham chiếu");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUND_PENDING);

        assertThatThrownBy(() -> service.processRefund(45L, 9L, "admin@example.invalid",
                new ManualRefundConfirmation("TEST-REF", BigDecimal.valueOf(99_999), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hoàn toàn bộ");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUND_PENDING);
        verify(bookings, never()).save(any(Booking.class));
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
