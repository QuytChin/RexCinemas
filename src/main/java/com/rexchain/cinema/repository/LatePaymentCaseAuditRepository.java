package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.LatePaymentCaseAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LatePaymentCaseAuditRepository extends JpaRepository<LatePaymentCaseAudit, Long> {
    List<LatePaymentCaseAudit> findByLatePaymentCaseIdOrderByCreatedAtDesc(Long caseId);
}
