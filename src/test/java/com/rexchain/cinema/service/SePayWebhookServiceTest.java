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
        properties.setBankAccount("0123456789");
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
        Booking booking = new Booking();
        booking.setBookingCode("REX-1");
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setPaymentReference("REXABC");
        booking.setStatus(status);
        booking.setTotalAmount(BigDecimal.valueOf(100_000));
        return booking;
    }

    private SePayWebhookRequest transaction(String id, String type, String code, String amount) {
        return new SePayWebhookRequest(id, "BANK", "2026-10-13T10:00:00", "0123456789", null,
                code, "Thanh toan " + code, type, new BigDecimal(amount), "REF-1");
    }
}
