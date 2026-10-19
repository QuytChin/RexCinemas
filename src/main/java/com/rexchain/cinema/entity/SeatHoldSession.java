package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One checkout clock per customer and showtime. Changing seats cannot extend it. */
@Entity
@Table(name = "seat_hold_sessions", uniqueConstraints =
        @UniqueConstraint(name = "uk_hold_session_user_showtime", columnNames = {"user_id", "showtime_id"}),
        indexes = @Index(name = "idx_hold_session_expires", columnList = "expires_at"))
public class SeatHoldSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime value) { showtime = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
