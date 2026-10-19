package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.RevokedAccessToken;
import com.rexchain.cinema.repository.RevokedAccessTokenRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessTokenRevocationService {
    private final RevokedAccessTokenRepository revokedTokens;

    public AccessTokenRevocationService(RevokedAccessTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Transactional
    public void revoke(String token, Instant expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null || !expiresAt.isAfter(Instant.now())) return;
        String hash = fingerprint(token);
        revokedTokens.deleteByExpiresAtBefore(Instant.now());
        if (!revokedTokens.existsById(hash)) {
            revokedTokens.save(new RevokedAccessToken(hash, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        return revokedTokens.existsByTokenHashAndExpiresAtAfter(fingerprint(token), Instant.now());
    }

    private String fingerprint(String token) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
