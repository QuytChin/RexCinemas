package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.PasswordResetChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetChallenge> findByEmailAndPurpose(String email, String purpose);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
