package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.time.LocalDateTime;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpChallenge> findByEmailAndPurpose(String email, String purpose);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
