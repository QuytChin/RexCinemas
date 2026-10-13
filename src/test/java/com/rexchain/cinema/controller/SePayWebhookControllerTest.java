package com.rexchain.cinema.controller;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.SePayWebhookRequest;
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
}
