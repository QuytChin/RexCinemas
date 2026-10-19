package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Reconciliation record kept separate from an EXPIRED booking and its released inventory. */
@Entity
@Table(name = "late_payment_cases", uniqueConstraints =
        @UniqueConstraint(name = "uk_late_payment_provider_transaction", columnNames = "provider_transaction_id"),
        indexes = {@Index(name = "idx_late_payment_status_created", columnList = "status,created_at"),
                @Index(name = "idx_late_payment_booking", columnList = "booking_id")})
public class LatePaymentCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private Booking booking;
    @Column(name = "provider_transaction_id", nullable = false, length = 120) private String providerTransactionId;
    @Column(length = 120) private String paymentReference;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    private LocalDateTime bankRecordedAt;
    private LocalDateTime webhookReceivedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private LatePaymentCaseStatus status = LatePaymentCaseStatus.PENDING_REFUND;
    @Column(length = 600) private String reason;
    private LocalDateTime refundedAt;
    @Column(length = 180) private String refundedBy;
    private Long refundedByUserId;
    @Column(length = 120) private String refundReference;
    @Column(precision = 12, scale = 2) private BigDecimal refundAmount;
    @Column(length = 600) private String refundEvidence;
    @Column(length = 255) private String refundNote;
    private LocalDateTime refundFailedAt;
    @Column(length = 255) private String refundFailureReason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Booking getBooking() { return booking; } public void setBooking(Booking value) { booking = value; }
    public String getProviderTransactionId() { return providerTransactionId; } public void setProviderTransactionId(String value) { providerTransactionId = value; }
    public String getPaymentReference() { return paymentReference; } public void setPaymentReference(String value) { paymentReference = value; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
    public LocalDateTime getBankRecordedAt() { return bankRecordedAt; } public void setBankRecordedAt(LocalDateTime value) { bankRecordedAt = value; }
    public LocalDateTime getWebhookReceivedAt() { return webhookReceivedAt; } public void setWebhookReceivedAt(LocalDateTime value) { webhookReceivedAt = value; }
    public LatePaymentCaseStatus getStatus() { return status; } public void setStatus(LatePaymentCaseStatus value) { status = value; }
    public String getReason() { return reason; } public void setReason(String value) { reason = value; }
    public LocalDateTime getRefundedAt() { return refundedAt; } public void setRefundedAt(LocalDateTime value) { refundedAt = value; }
    public String getRefundedBy() { return refundedBy; } public void setRefundedBy(String value) { refundedBy = value; }
    public Long getRefundedByUserId() { return refundedByUserId; } public void setRefundedByUserId(Long value) { refundedByUserId = value; }
    public String getRefundReference() { return refundReference; } public void setRefundReference(String value) { refundReference = value; }
    public BigDecimal getRefundAmount() { return refundAmount; } public void setRefundAmount(BigDecimal value) { refundAmount = value; }
    public String getRefundEvidence() { return refundEvidence; } public void setRefundEvidence(String value) { refundEvidence = value; }
    public String getRefundNote() { return refundNote; } public void setRefundNote(String value) { refundNote = value; }
    public LocalDateTime getRefundFailedAt() { return refundFailedAt; } public void setRefundFailedAt(LocalDateTime value) { refundFailedAt = value; }
    public String getRefundFailureReason() { return refundFailureReason; } public void setRefundFailureReason(String value) { refundFailureReason = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
