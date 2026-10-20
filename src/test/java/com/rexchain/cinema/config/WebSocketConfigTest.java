package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConfigTest {
    @Test
    void allowsOnlyConfiguredAndLocalOrigins() {
        WebSocketConfig config = new WebSocketConfig("https://cinema.example", "https://public.example/path");
        assertThat(config.allowedOrigins()).contains("http://localhost:8080", "https://cinema.example", "https://public.example")
                .doesNotContain("*");
    }

    @Test
    void rejectsClientSendsAndNonSeatSubscriptions() {
        WebSocketConfig config = new WebSocketConfig("", "");
        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/showtimes/12/seats");
        assertThatThrownBy(() -> config.validateInbound(StompCommand.SEND, "/app/anything"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> config.validateInbound(StompCommand.SUBSCRIBE, "/topic/private"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
