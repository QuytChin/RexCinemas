package com.rexchain.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.service.SePayWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SePayWebhookHmacTest {
    @Test
    void acceptsOfficialHmacFormatUsingRawBodyAndTimestamp() throws Exception {
        SePayProperties properties = new SePayProperties();
        properties.setWebhookAuthMode("HMAC_SHA256");
        properties.setWebhookSecret("test-secret");
        SePayWebhookController controller = new SePayWebhookController(properties,
                mock(SePayWebhookService.class), new ObjectMapper());
        byte[] body = "{\"id\":\"tx-1\",\"transferType\":\"in\",\"transferAmount\":100}".getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = hmac("test-secret", timestamp + "." + new String(body, StandardCharsets.UTF_8));

        assertThat(controller.webhook(null, null, "sha256=" + signature, timestamp, body).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectsWrongSignatureAndExpiredTimestampBeforeService() throws Exception {
        SePayProperties properties = new SePayProperties();
        properties.setWebhookAuthMode("HMAC_SHA256");
        properties.setWebhookSecret("test-secret");
        SePayWebhookService service = mock(SePayWebhookService.class);
        SePayWebhookController controller = new SePayWebhookController(properties, service, new ObjectMapper());
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String oldTimestamp = String.valueOf(Instant.now().minusSeconds(301).getEpochSecond());

        assertThat(controller.webhook(null, null, "sha256=bad", oldTimestamp, body).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    private String hmac(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
