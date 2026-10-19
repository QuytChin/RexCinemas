package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.RevokedAccessToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    long deleteByExpiresAtBefore(Instant cutoff);
    @Transactional
    void deleteByTokenHash(String tokenHash);
}
