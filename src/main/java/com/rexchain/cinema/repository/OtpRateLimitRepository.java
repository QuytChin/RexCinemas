package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.OtpRateLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OtpRateLimitRepository extends JpaRepository<OtpRateLimit, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpRateLimit> findByScopeKeyAndPurpose(String scopeKey, String purpose);
}
