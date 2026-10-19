package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_notifications", uniqueConstraints =
        @UniqueConstraint(name = "uk_account_notification_dedupe", columnNames = {"user_id", "deduplication_key"}),
        indexes = @Index(name = "idx_account_notifications_user_created", columnList = "user_id,created_at"))
public class AccountNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 500) private String message;
    @Column(nullable = false, length = 30) private String type;
    @Column(length = 255) private String link;
    @Column(nullable = false) private boolean readAtPresent;
    private LocalDateTime readAt;
    @Column(name = "deduplication_key", nullable = false, length = 160) private String deduplicationKey;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getLink() { return link; } public void setLink(String link) { this.link = link; }
    public LocalDateTime getReadAt() { return readAt; }
    public boolean isRead() { return readAtPresent; }
    public void markRead() { readAtPresent = true; readAt = LocalDateTime.now(); }
    public String getDeduplicationKey() { return deduplicationKey; } public void setDeduplicationKey(String v) { this.deduplicationKey = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
