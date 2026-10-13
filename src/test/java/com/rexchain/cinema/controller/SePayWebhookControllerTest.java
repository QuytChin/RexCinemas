package com.rexchain.cinema.controller;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.SePayWebhookRequest;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.service.BookingService;
import com.rexchain.cinema.service.SePayWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SePayWebhookControllerTest {
    @Mock SePayWebhookService service;
    @Mock BookingRepository bookings;
    @Mock BookingService bookingService;
    private SePayWebhookController controller;

    @BeforeEach
    void setUp() {
        SePayProperties properties = new SePayProperties();
        properties.setApiKey("test-api-key");
        controller = new SePayWebhookController(properties, service);
    }

    @Test
    void missingOrInvalidApiKeyReturnsUnauthorized() {
        assertThat(controller.webhook(null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.webhook("Bearer test-api-key", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(service);
    }

    @Test
    void validApiKeyAcknowledgesWebhook() {
        SePayWebhookRequest request = new SePayWebhookRequest("tx", "BANK", null, "0123", null,
                "REXABC", "REXABC", "in", BigDecimal.ONE, "ref");
        ResponseEntity<?> response = controller.webhook("Apikey test-api-key", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("success", true));
        verify(service).process(request);
    }

    @Test
    void contentFallbackAcknowledgesConfirmedRealWebhookPayload() {
        Booking booking = new Booking();
        booking.setBookingCode("REX7B978F6FAD");
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setPaymentReference("REXELEL7PKS");
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(BigDecimal.valueOf(2_000));
        when(bookings.findBySepayTransactionId("999002")).thenReturn(java.util.Optional.empty());
        when(bookings.findByPaymentMethodAndStatus(PaymentMethod.BANK_QR, BookingStatus.PENDING))
                .thenReturn(java.util.List.of(booking));

        SePayProperties realProperties = new SePayProperties();
        realProperties.setApiKey("test-api-key");
        realProperties.setBankAccount("0000501454103");
        SePayWebhookController realController = new SePayWebhookController(realProperties,
                new SePayWebhookService(bookings, bookingService, realProperties));
        SePayWebhookRequest request = new SePayWebhookRequest(
                "999002", "MBBank", "2026-10-13T10:00:00", "0000501454103", null,
                null, "TEST-REXELEL7PKS-CHUYEN TIEN", "in", BigDecimal.valueOf(2_000),
                "LOCAL_TEST_999002");

        ResponseEntity<?> response = realController.webhook("Apikey test-api-key", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("success", true));
        verify(bookingService).completeOnlinePayment("REX7B978F6FAD", "999002");
    }
}
