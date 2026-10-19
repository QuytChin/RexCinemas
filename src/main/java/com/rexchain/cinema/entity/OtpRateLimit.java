package com.rexchain.cinema.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_rate_limits",
        uniqueConstraints = @UniqueConstraint(name = "uk_otp_rate_scope_purpose", columnNames = {"scope_key", "purpose"}))
public class OtpRateLimit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version = 0L;
    @Column(name = "scope_key", nullable = false, length = 220)
    private String scopeKey;
    @Column(nullable = false, length = 40)
    private String purpose;
    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;
    @Column(name = "window_count", nullable = false)
    private int windowCount;
    @Column(name = "next_allowed_at")
    private LocalDateTime nextAllowedAt;

    public Long getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String value) { scopeKey = value; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String value) { purpose = value; }
    public LocalDateTime getWindowStartedAt() { return windowStartedAt; }
    public void setWindowStartedAt(LocalDateTime value) { windowStartedAt = value; }
    public int getWindowCount() { return windowCount; }
    public void setWindowCount(int value) { windowCount = Math.max(0, value); }
    public LocalDateTime getNextAllowedAt() { return nextAllowedAt; }
    public void setNextAllowedAt(LocalDateTime value) { nextAllowedAt = value; }
}
