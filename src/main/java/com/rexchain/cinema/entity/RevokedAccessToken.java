package com.rexchain.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "revoked_access_tokens", indexes =
        @Index(name = "idx_revoked_access_tokens_expires_at", columnList = "expires_at"))
public class RevokedAccessToken {
    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedAccessToken() {}

    public RevokedAccessToken(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
}
