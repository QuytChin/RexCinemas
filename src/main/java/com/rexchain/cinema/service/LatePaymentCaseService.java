package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.ManualRefundConfirmation;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.LatePaymentCaseRepository;
import com.rexchain.cinema.repository.LatePaymentCaseAuditRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LatePaymentCaseService {
    private final LatePaymentCaseRepository cases;
    private final LatePaymentCaseAuditRepository audits;
    private final NotificationService notifications;

    public LatePaymentCaseService(LatePaymentCaseRepository cases, LatePaymentCaseAuditRepository audits,
                                  NotificationService notifications) {
        this.cases = cases; this.audits = audits; this.notifications = notifications;
    }

    @Transactional
    public LatePaymentCase open(Booking booking, String providerTransactionId, BigDecimal amount,
                                LocalDateTime bankRecordedAt, String reason) {
        LatePaymentCase existing = cases.findByProviderTransactionId(providerTransactionId).orElse(null);
        if (existing != null) return existing;
        LatePaymentCase value = new LatePaymentCase();
        value.setBooking(booking); value.setProviderTransactionId(providerTransactionId);
        value.setPaymentReference(booking.getPaymentReference()); value.setAmount(amount);
        value.setBankRecordedAt(bankRecordedAt); value.setWebhookReceivedAt(LocalDateTime.now());
        value.setReason(reason); value.setStatus(LatePaymentCaseStatus.PENDING_REFUND);
        LatePaymentCase saved = cases.save(value);
        audit(saved, "OPENED", null, reason);
        notifications.latePaymentPending(booking, providerTransactionId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LatePaymentCase> openCases() {
        return cases.findByStatusInOrderByCreatedAtDesc(List.of(LatePaymentCaseStatus.PENDING_REFUND,
                LatePaymentCaseStatus.NEEDS_REVIEW, LatePaymentCaseStatus.REFUND_FAILED));
    }

    @Transactional(readOnly = true)
    public List<LatePaymentCase> search(LatePaymentCaseStatus status, String query) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return cases.search(status, normalized);
    }

    @Transactional(readOnly = true)
    public List<LatePaymentCaseAudit> auditHistory(Long caseId) {
        return audits.findByLatePaymentCaseIdOrderByCreatedAtDesc(caseId);
    }

    @Transactional(readOnly = true)
    public LatePaymentCase findDetailed(Long caseId) {
        return cases.findDetailedById(caseId).orElseThrow();
    }

    @Transactional
    public LatePaymentCase markNeedsReview(Long caseId, Long operatorId, String operatorEmail, String note) {
        LatePaymentCase value = cases.findByIdForUpdate(caseId).orElseThrow();
        if (value.getStatus() == LatePaymentCaseStatus.REFUNDED) return value;
        value.setStatus(LatePaymentCaseStatus.NEEDS_REVIEW);
        LatePaymentCase saved = cases.save(value);
        audit(saved, "MARKED_IN_REVIEW", operatorEmail, note);
        return saved;
    }

    @Transactional
    public LatePaymentCase confirmRefund(Long caseId, Long operatorId, String operatorEmail,
                                         ManualRefundConfirmation confirmation) {
        LatePaymentCase value = cases.findByIdForUpdate(caseId).orElseThrow();
        if (value.getStatus() == LatePaymentCaseStatus.REFUNDED) return value;
        validate(value, operatorId, operatorEmail, confirmation);
        value.setStatus(LatePaymentCaseStatus.REFUNDED); value.setRefundedAt(LocalDateTime.now());
        value.setRefundedByUserId(operatorId); value.setRefundedBy(operatorEmail);
        value.setRefundReference(confirmation.transferReference()); value.setRefundAmount(confirmation.amount());
        value.setRefundEvidence(confirmation.evidence()); value.setRefundNote(confirmation.note());
        LatePaymentCase saved = cases.save(value);
        audit(saved, "REFUND_CONFIRMED", operatorEmail, confirmation.transferReference());
        notifications.latePaymentRefunded(saved.getBooking());
        return saved;
    }

    @Transactional
    public LatePaymentCase markRefundFailed(Long caseId, Long operatorId, String operatorEmail, String reason) {
        LatePaymentCase value = cases.findByIdForUpdate(caseId).orElseThrow();
        if (value.getStatus() == LatePaymentCaseStatus.REFUNDED) return value;
        if (reason == null || reason.isBlank() || reason.trim().length() > 255)
            throw new IllegalArgumentException("Lý do hoàn tiền thất bại là bắt buộc, tối đa 255 ký tự");
        value.setStatus(LatePaymentCaseStatus.REFUND_FAILED);
        value.setRefundFailedAt(LocalDateTime.now()); value.setRefundFailureReason(reason.trim());
        LatePaymentCase saved = cases.save(value);
        audit(saved, "REFUND_FAILED", operatorEmail, reason.trim());
        return saved;
    }

    private void validate(LatePaymentCase value, Long operatorId, String operatorEmail,
                          ManualRefundConfirmation confirmation) {
        if (operatorId == null || operatorEmail == null || operatorEmail.isBlank())
            throw new IllegalArgumentException("Không xác định được quản trị viên xác nhận hoàn tiền");
        if (confirmation == null || confirmation.transferReference() == null || confirmation.transferReference().isBlank())
            throw new IllegalArgumentException("Phải nhập mã tham chiếu hoặc chứng từ chuyển khoản hoàn tiền");
        if (confirmation.amount() == null || confirmation.amount().compareTo(value.getAmount()) != 0)
            throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ hoàn toàn bộ đúng bằng giao dịch tiền đến muộn");
        if (confirmation.transferReference().length() > 120 || (confirmation.note() != null && confirmation.note().length() > 255)
                || (confirmation.evidence() != null && confirmation.evidence().length() > 600))
            throw new IllegalArgumentException("Dữ liệu chứng từ hoàn tiền không hợp lệ");
    }

    private void audit(LatePaymentCase value, String action, String actor, String detail) {
        LatePaymentCaseAudit audit = new LatePaymentCaseAudit();
        audit.setLatePaymentCase(value); audit.setAction(action); audit.setActor(actor);
        audit.setDetail(detail == null || detail.isBlank() ? null : detail.trim());
        audits.save(audit);
    }
}
