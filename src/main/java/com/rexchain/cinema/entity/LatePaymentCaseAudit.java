package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable audit trail for staff actions on a late incoming payment. */
@Entity
@Table(name = "late_payment_case_audits", indexes =
        @Index(name = "idx_late_payment_audit_case_created", columnList = "late_payment_case_id,created_at"))
public class LatePaymentCaseAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "late_payment_case_id", nullable = false)
    private LatePaymentCase latePaymentCase;
    @Column(nullable = false, length = 40) private String action;
    @Column(length = 180) private String actor;
    @Column(length = 600) private String detail;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public LatePaymentCase getLatePaymentCase() { return latePaymentCase; }
    public void setLatePaymentCase(LatePaymentCase value) { latePaymentCase = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getActor() { return actor; }
    public void setActor(String value) { actor = value; }
    public String getDetail() { return detail; }
    public void setDetail(String value) { detail = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
