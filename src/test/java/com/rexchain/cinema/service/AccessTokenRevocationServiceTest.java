package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.RevokedAccessToken;
import com.rexchain.cinema.repository.RevokedAccessTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AccessTokenRevocationServiceTest {
    private final RevokedAccessTokenRepository repository = mock(RevokedAccessTokenRepository.class);
    private final AccessTokenRevocationService service = new AccessTokenRevocationService(repository);

    @Test
    void persistsOnlySha256FingerprintAndExpiryForLogout() throws Exception {
        String jwt = "test.jwt.value";
        Instant expiresAt = Instant.now().plusSeconds(600);
        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(jwt.getBytes(StandardCharsets.UTF_8)));

        service.revoke(jwt, expiresAt);

        verify(repository).save(argThat(token -> token.getTokenHash().equals(expectedHash)
                && token.getExpiresAt().equals(expiresAt)));
        verify(repository, never()).save(argThat(token -> token.getTokenHash().equals(jwt)));
    }

    @Test
    void revokedCheckUsesFingerprintAndExpiredTokensAreNotPersisted() throws Exception {
        String jwt = "test.jwt.value";
        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(jwt.getBytes(StandardCharsets.UTF_8)));
        when(repository.existsByTokenHashAndExpiresAtAfter(eq(expectedHash), any(Instant.class))).thenReturn(true);

        assertThat(service.isRevoked(jwt)).isTrue();
        service.revoke(jwt, Instant.now().minusSeconds(1));

        verify(repository).existsByTokenHashAndExpiresAtAfter(eq(expectedHash), any(Instant.class));
        verify(repository, never()).save(any(RevokedAccessToken.class));
    }

    @Test
    void repeatedLogoutDoesNotInsertDuplicateRevocation() {
        String jwt = "same-token";
        String hash = fingerprint(jwt);
        when(repository.existsById(hash)).thenReturn(true);

        service.revoke(jwt, Instant.now().plusSeconds(600));

        verify(repository, never()).save(any(RevokedAccessToken.class));
    }

    private String fingerprint(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
