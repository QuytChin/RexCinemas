package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.SePayWebhookRequest;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SePayWebhookServiceTest {
    @Mock BookingRepository bookings;
    @Mock BookingService bookingService;
    private SePayWebhookService service;
    private SePayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SePayProperties();
        properties.setBankAccount("0000000000000");
        service = new SePayWebhookService(bookings, bookingService, properties);
    }

    @Test
    void validIncomingAmountCompletesBankQrBooking() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookings.findBySepayTransactionId("tx-1")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXABC"))
                .thenReturn(Optional.of(booking));

        service.process(transaction("tx-1", "in", "REXABC", "100000"));

        verify(bookingService).completeOnlinePayment("REX-1", "tx-1");
    }

    @Test
    void wrongAmountDoesNotCompleteBooking() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookings.findBySepayTransactionId("tx-2")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXABC"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.process(transaction("tx-2", "in", "REXABC", "999")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(bookingService);
    }

    @Test
    void nullCodeResolvesPaymentReferenceFromContent() {
        Booking booking = bookingWithReference("REXELEL7PKS", BookingStatus.PENDING, BigDecimal.valueOf(2_000));
        when(bookings.findBySepayTransactionId("999002")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXELEL7PKS"))
                .thenReturn(Optional.of(booking));

        service.process(new SePayWebhookRequest(
                "999002", "MBBank", "2026-10-13T10:00:00", "0000000000000", null,
                null, "TEST-REXELEL7PKS-CHUYEN TIEN", "in", BigDecimal.valueOf(2_000),
                "LOCAL_TEST_999002"));

        verify(bookingService).completeOnlinePayment("REX7B978F6FAD", "999002");
    }

    @Test
    void invalidCodeFallsBackToPaymentReferenceInContent() {
        Booking booking = bookingWithReference("REXELEL7PKS", BookingStatus.PENDING, BigDecimal.valueOf(2_000));
        when(bookings.findBySepayTransactionId("tx-content-fallback")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "WRONG"))
                .thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXELEL7PKS"))
                .thenReturn(Optional.of(booking));

        service.process(new SePayWebhookRequest(
                "tx-content-fallback", "MBBank", null, "0000000000000", null,
                "WRONG", "TEST-REXELEL7PKS-CHUYEN TIEN", "in", BigDecimal.valueOf(2_000), null));

        verify(bookingService).completeOnlinePayment("REX7B978F6FAD", "tx-content-fallback");
    }

    @Test
    void contentContainingOnlyAPartialReferenceTokenDoesNotSettleABooking() {
        when(bookings.findBySepayTransactionId("tx-substring")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXELEL7PKSX"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(new SePayWebhookRequest(
                "tx-substring", "MBBank", null, "0000000000000", null,
                null, "TEST-REXELEL7PKSX-CHUYEN TIEN", "in", BigDecimal.valueOf(2_000), null)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bookingService);
    }

    @Test
    void contentWithoutKnownPaymentReferenceIsRejected() {
        when(bookings.findBySepayTransactionId("tx-no-reference")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "BOOKING"))
                .thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "CHUYEN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(new SePayWebhookRequest(
                "tx-no-reference", "MBBank", null, "0123456789", null,
                null, "TEST-NOT-A-BOOKING-CHUYEN TIEN", "in", BigDecimal.valueOf(2_000), null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(bookingService);
    }

    @Test
    void duplicateTransactionIdIsIgnored() {
        when(bookings.findBySepayTransactionId("tx-3")).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));

        service.process(transaction("tx-3", "in", "REXABC", "100000"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void confirmedBookingIsNeverSettledAgain() {
        Booking booking = booking(BookingStatus.CONFIRMED);
        when(bookings.findBySepayTransactionId("tx-4")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXABC"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.process(transaction("tx-4", "in", "REXABC", "100000")))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(bookingService);
    }

    @Test
    void expiredBookingIsDelegatedToRefundHandling() {
        Booking booking = booking(BookingStatus.EXPIRED);
        when(bookings.findBySepayTransactionId("tx-5")).thenReturn(Optional.empty());
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXABC"))
                .thenReturn(Optional.of(booking));

        service.process(transaction("tx-5", "in", "REXABC", "100000"));

        verify(bookingService).completeOnlinePayment("REX-1", "tx-5");
    }

    private Booking booking(BookingStatus status) {
        return bookingWithReference("REXABC", status, BigDecimal.valueOf(100_000));
    }

    private Booking bookingWithReference(String reference, BookingStatus status, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setBookingCode(reference.equals("REXELEL7PKS") ? "REX7B978F6FAD" : "REX-1");
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setPaymentReference(reference);
        booking.setStatus(status);
        booking.setTotalAmount(amount);
        return booking;
    }

    private SePayWebhookRequest transaction(String id, String type, String code, String amount) {
        return new SePayWebhookRequest(id, "BANK", "2026-10-13T10:00:00", "0000000000000", null,
                code, "Thanh toan " + code, type, new BigDecimal(amount), "REF-1");
    }
}
