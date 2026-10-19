package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.RewardRedemption;
import com.rexchain.cinema.entity.RewardRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    Optional<RewardRedemption> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    Optional<RewardRedemption> findByVoucherId(Long voucherId);
    List<RewardRedemption> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByStatus(RewardRedemptionStatus status);
}
