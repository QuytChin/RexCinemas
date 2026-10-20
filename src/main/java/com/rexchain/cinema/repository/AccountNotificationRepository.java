package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.AccountNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountNotificationRepository extends JpaRepository<AccountNotification, Long> {
    boolean existsByUserIdAndDeduplicationKey(Long userId, String deduplicationKey);
    long countByUserIdAndReadAtPresentFalse(Long userId);
    Page<AccountNotification> findByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);
    Optional<AccountNotification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update AccountNotification notification set notification.readAtPresent = true, notification.readAt = :readAt " +
            "where notification.user.id = :userId and notification.readAtPresent = false")
    int markAllReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
