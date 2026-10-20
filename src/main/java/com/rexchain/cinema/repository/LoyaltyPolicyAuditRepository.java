package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.LoyaltyPolicyAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoyaltyPolicyAuditRepository extends JpaRepository<LoyaltyPolicyAudit, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<LoyaltyPolicyAudit> {
    List<LoyaltyPolicyAudit> findTop50ByOrderByCreatedAtDesc();
}
