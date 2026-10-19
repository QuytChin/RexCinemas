package com.rexchain.cinema.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_challenges",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_email_purpose", columnNames = {"email", "purpose"}),
        indexes = {
                @Index(name = "idx_password_reset_expires", columnList = "expires_at"),
                @Index(name = "idx_password_reset_token_expires", columnList = "reset_token_expires_at")
        })
public class PasswordResetChallenge {
    public static final String PURPOSE = "RESET_PASSWORD";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version = 0L;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false, length = 40)
    private String purpose = PURPOSE;
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;
    @Column(name = "otp_salt", nullable = false, length = 64)
    private String otpSalt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    @Column(name = "next_send_at")
    private LocalDateTime nextSendAt;
    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;
    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;
    @Column(name = "client_ip", length = 64)
    private String clientIp;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void beforeInsert() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    void beforeUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String value) { purpose = value; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String value) { otpHash = value; }
    public String getOtpSalt() { return otpSalt; }
    public void setOtpSalt(String value) { otpSalt = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int value) { failedAttempts = Math.max(0, value); }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime value) { lockedUntil = value; }
    public LocalDateTime getNextSendAt() { return nextSendAt; }
    public void setNextSendAt(LocalDateTime value) { nextSendAt = value; }
    public boolean isOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean value) { otpVerified = value; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime value) { verifiedAt = value; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String value) { resetTokenHash = value; }
    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(LocalDateTime value) { resetTokenExpiresAt = value; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String value) { clientIp = value; }
}
