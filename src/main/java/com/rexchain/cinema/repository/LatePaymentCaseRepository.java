package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.LatePaymentCase;
import com.rexchain.cinema.entity.LatePaymentCaseStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LatePaymentCaseRepository extends JpaRepository<LatePaymentCase, Long> {
    Optional<LatePaymentCase> findByProviderTransactionId(String providerTransactionId);
    List<LatePaymentCase> findByStatusInOrderByCreatedAtDesc(List<LatePaymentCaseStatus> statuses);
    @Query("select c from LatePaymentCase c join fetch c.booking b join fetch b.user " +
            "where (:status is null or c.status = :status) and " +
            "(:query is null or lower(b.bookingCode) like lower(concat('%', :query, '%')) " +
            "or lower(c.providerTransactionId) like lower(concat('%', :query, '%')) " +
            "or lower(b.user.email) like lower(concat('%', :query, '%'))) order by c.createdAt desc")
    List<LatePaymentCase> search(@Param("status") LatePaymentCaseStatus status, @Param("query") String query);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LatePaymentCase c join fetch c.booking where c.id = :id")
    Optional<LatePaymentCase> findByIdForUpdate(@Param("id") Long id);
    @Query("select c from LatePaymentCase c join fetch c.booking b join fetch b.user where c.id = :id")
    Optional<LatePaymentCase> findDetailedById(@Param("id") Long id);
}
