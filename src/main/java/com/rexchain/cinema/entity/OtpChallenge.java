package com.rexchain.cinema.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_challenges",
        uniqueConstraints = @UniqueConstraint(name = "uk_otp_challenge_email_purpose", columnNames = {"email", "purpose"}),
        indexes = {
                @Index(name = "idx_otp_challenge_expires", columnList = "expires_at"),
                @Index(name = "idx_otp_challenge_locked", columnList = "locked_until")
        })
public class OtpChallenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version = 0L;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false, length = 40)
    private String purpose;
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;
    @Column(name = "otp_salt", nullable = false, length = 64)
    private String otpSalt;
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, length = 30)
    private String phone;
    @Column(nullable = false, length = 20)
    private String gender;
    private LocalDate birthDate;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;
    @Column(length = 50)
    private String username;
    @Column(name = "national_id", length = 12)
    private String nationalId;
    @Column(name = "favorite_cinema_id")
    private Long favoriteCinemaId;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    @Column(name = "next_send_at")
    private LocalDateTime nextSendAt;
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
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getGender() { return gender; }
    public void setGender(String value) { gender = value; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate value) { birthDate = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean value) { termsAccepted = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String value) { nationalId = value; }
    public Long getFavoriteCinemaId() { return favoriteCinemaId; }
    public void setFavoriteCinemaId(Long value) { favoriteCinemaId = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int value) { failedAttempts = Math.max(0, value); }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime value) { lockedUntil = value; }
    public LocalDateTime getNextSendAt() { return nextSendAt; }
    public void setNextSendAt(LocalDateTime value) { nextSendAt = value; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String value) { clientIp = value; }
}
