package com.rexchain.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.SePayWebhookRequest;
import com.rexchain.cinema.service.SePayWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/sepay")
public class SePayWebhookController {
    private final SePayProperties properties;
    private final SePayWebhookService webhookService;
    private final ObjectMapper objectMapper;

    /** Compatibility constructor for direct controller tests. */
    public SePayWebhookController(SePayProperties properties, SePayWebhookService webhookService) {
        this(properties, webhookService, new ObjectMapper());
    }

    @Autowired
    public SePayWebhookController(SePayProperties properties, SePayWebhookService webhookService,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(HttpServletRequest request,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                     @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
                                     @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp,
                                     @RequestBody(required = false) byte[] rawBody) {
        if (!authorized(authorization, signature, timestamp, rawBody))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        try {
            if (rawBody == null || rawBody.length == 0)
                throw new IllegalArgumentException("Thiếu payload webhook");
            SePayWebhookRequest transaction = objectMapper.readValue(rawBody, SePayWebhookRequest.class);
            webhookService.process(transaction);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (java.io.IOException | IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("success", false));
        }
    }

    /** Compatibility entry point for existing unit tests; production traffic uses raw-body verification above. */
    public ResponseEntity<?> webhook(String authorization, SePayWebhookRequest transaction) {
        if (!authorizedApiKey(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            webhookService.process(transaction);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("success", false));
        }
    }

    private boolean authorized(String authorization, String signature, String timestamp, byte[] rawBody) {
        if ("HMAC_SHA256".equalsIgnoreCase(properties.getWebhookAuthMode()))
            return authorizedHmac(signature, timestamp, rawBody);
        return authorizedApiKey(authorization);
    }

    private boolean authorizedApiKey(String authorization) {
        if (authorization == null || !authorization.startsWith("Apikey ")) return false;
        String supplied = authorization.substring("Apikey ".length()).trim();
        String expected = properties.getApiKey();
        if (supplied.isEmpty() || expected == null || expected.isBlank()) return false;
        return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                expected.trim().getBytes(StandardCharsets.UTF_8));
    }

    private boolean authorizedHmac(String signature, String timestamp, byte[] rawBody) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank() || signature == null || timestamp == null || rawBody == null)
            return false;
        long suppliedTimestamp;
        try { suppliedTimestamp = Long.parseLong(timestamp.trim()); }
        catch (NumberFormatException ex) { return false; }
        int tolerance = properties.getWebhookTimestampToleranceSeconds() > 0
                ? properties.getWebhookTimestampToleranceSeconds() : 300;
        if (Math.abs(Instant.now().getEpochSecond() - suppliedTimestamp) > tolerance) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signed = (timestamp.trim() + ".").getBytes(StandardCharsets.UTF_8);
            byte[] payload = new byte[signed.length + rawBody.length];
            System.arraycopy(signed, 0, payload, 0, signed.length);
            System.arraycopy(rawBody, 0, payload, signed.length, rawBody.length);
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                    signature.trim().getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ex) { return false; }
    }
}
