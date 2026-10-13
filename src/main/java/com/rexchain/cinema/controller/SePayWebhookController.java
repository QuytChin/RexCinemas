package com.rexchain.cinema.controller;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.SePayWebhookRequest;
import com.rexchain.cinema.service.SePayWebhookService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/sepay")
public class SePayWebhookController {
    private final SePayProperties properties;
    private final SePayWebhookService webhookService;

    public SePayWebhookController(SePayProperties properties, SePayWebhookService webhookService) {
        this.properties = properties;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                     @RequestBody(required = false) SePayWebhookRequest transaction) {
        if (!authorized(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            webhookService.process(transaction);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("success", false));
        }
    }

    private boolean authorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Apikey ")) return false;
        String supplied = authorization.substring("Apikey ".length()).trim();
        String expected = properties.getApiKey();
        if (supplied.isEmpty() || expected == null || expected.isBlank()) return false;
        return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                expected.trim().getBytes(StandardCharsets.UTF_8));
    }
}
