package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedgerEntry, Long> {
    boolean existsByReference(String reference);
    Optional<LoyaltyLedgerEntry> findByReference(String reference);
    Page<LoyaltyLedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<LoyaltyLedgerEntry> findByTransactionTypeOrderByCreatedAtDesc(LoyaltyTransactionType type, Pageable pageable);
    Page<LoyaltyLedgerEntry> findByUserIdAndTransactionTypeInOrderByCreatedAtDesc(Long userId, Collection<LoyaltyTransactionType> types, Pageable pageable);
    List<LoyaltyLedgerEntry> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByTransactionType(LoyaltyTransactionType type);
}
