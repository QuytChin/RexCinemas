package com.rexchain.cinema.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    @Test
    void rejectsMissingOrShortSigningKeyWithoutEchoingIt() {
        assertThatThrownBy(() -> new JwtService("", 60_000, 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET is required and must contain at least 32 UTF-8 bytes");
        assertThatThrownBy(() -> new JwtService("short-key", 60_000, 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 UTF-8 bytes");
    }

    @Test
    void acceptsValidKeyAndIssuesVerifiableToken() {
        JwtService service = new JwtService("test-only-key-material-with-at-least-32-bytes", 60_000, 120_000);

        String token = service.generate("member@example.invalid", "CUSTOMER");

        assertThat(service.valid(token)).isTrue();
        assertThat(service.extractEmail(token)).isEqualTo("member@example.invalid");
    }
}
