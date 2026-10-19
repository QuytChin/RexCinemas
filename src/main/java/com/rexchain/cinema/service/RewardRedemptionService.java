package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class RewardRedemptionService {
    private final RewardRepository rewards;
    private final RewardRedemptionRepository redemptions;
    private final VoucherRepository vouchers;
    private final LoyaltyService loyalty;
    private final MembershipService membership;

    public RewardRedemptionService(RewardRepository rewards, RewardRedemptionRepository redemptions,
                                   VoucherRepository vouchers, LoyaltyService loyalty, MembershipService membership) {
        this.rewards = rewards; this.redemptions = redemptions; this.vouchers = vouchers;
        this.loyalty = loyalty; this.membership = membership;
    }

    @Transactional
    public RewardRedemption redeem(User user, Long rewardId, String requestId) {
        if (!membership.settings().isRedemptionEnabled()) throw new IllegalStateException("Chương trình đổi điểm đang tạm dừng");
        String key = normalizeRequestKey(requestId);
        RewardRedemption existing = redemptions.findByUserIdAndIdempotencyKey(user.getId(), key).orElse(null);
        if (existing != null) return existing;
        Reward reward = rewards.findByIdForUpdate(rewardId).orElseThrow();
        if (!reward.isActive()) throw new IllegalStateException("Ưu đãi không còn khả dụng");
        if (reward.getRedemptionLimit() != null && reward.getRedeemedCount() >= reward.getRedemptionLimit())
            throw new IllegalStateException("Ưu đãi đã hết lượt đổi");
        // Recheck after acquiring the reward lock so repeated submits cannot double-spend.
        existing = redemptions.findByUserIdAndIdempotencyKey(user.getId(), key).orElse(null);
        if (existing != null) return existing;
        Voucher voucher = new Voucher();
        voucher.setCode(newVoucherCode());
        voucher.setName(reward.getName());
        voucher.setDiscountType(DiscountType.FIXED);
        voucher.setDiscountValue(reward.getVoucherAmount());
        voucher.setMinOrderAmount(reward.getVoucherMinOrderAmount() == null ? BigDecimal.ZERO : reward.getVoucherMinOrderAmount());
        voucher.setUsageLimit(1); voucher.setUsedCount(0); voucher.setActive(true);
        voucher.setValidFrom(LocalDateTime.now());
        voucher.setValidUntil(LocalDateTime.now().plusDays(reward.getValidityDays()));
        voucher.setOwnerUser(user);
        voucher = vouchers.save(voucher);
        RewardRedemption redemption = new RewardRedemption();
        redemption.setUser(user); redemption.setReward(reward); redemption.setVoucher(voucher);
        redemption.setIdempotencyKey(key); redemption.setPointsCost(reward.getPointsCost());
        redemption = redemptions.save(redemption);
        voucher.setRewardRedemptionReference("REWARD:" + redemption.getId());
        vouchers.save(voucher);
        loyalty.redeem(user, reward.getPointsCost(), redemption, "REDEEM:" + user.getId() + ":" + key);
        reward.setRedeemedCount(reward.getRedeemedCount() + 1); reward.setUpdatedAt(LocalDateTime.now());
        rewards.save(reward);
        return redemption;
    }

    @Transactional
    public void markVoucherUsed(Voucher voucher) {
        if (voucher == null) return;
        redemptions.findByVoucherId(voucher.getId()).ifPresent(redemption -> {
            if (redemption.getStatus() == RewardRedemptionStatus.ISSUED) {
                redemption.setStatus(RewardRedemptionStatus.USED); redemption.setUsedAt(LocalDateTime.now());
                redemptions.save(redemption);
            }
        });
    }

    @Transactional
    public void restoreRewardVoucher(Voucher voucher) {
        if (voucher == null) return;
        redemptions.findByVoucherId(voucher.getId()).ifPresent(redemption -> {
            if (redemption.getStatus() == RewardRedemptionStatus.USED) {
                redemption.setStatus(RewardRedemptionStatus.ISSUED); redemption.setUsedAt(null);
                redemptions.save(redemption);
            }
        });
    }

    @Transactional
    public Reward saveReward(Long id, String name, String description, int pointsCost, BigDecimal voucherAmount,
                             BigDecimal minOrder, int validityDays, Integer redemptionLimit, boolean active) {
        if (name == null || name.isBlank() || name.trim().length() > 160) throw new IllegalArgumentException("Tên ưu đãi không hợp lệ");
        if (pointsCost < 1 || pointsCost > 1_000_000) throw new IllegalArgumentException("Điểm cần đổi không hợp lệ");
        if (voucherAmount == null || voucherAmount.compareTo(BigDecimal.ONE) < 0) throw new IllegalArgumentException("Mệnh giá voucher không hợp lệ");
        if (minOrder != null && minOrder.signum() < 0) throw new IllegalArgumentException("Đơn tối thiểu không hợp lệ");
        if (validityDays < 1 || validityDays > 365) throw new IllegalArgumentException("Hạn dùng phải từ 1 đến 365 ngày");
        if (redemptionLimit != null && redemptionLimit < 1) throw new IllegalArgumentException("Giới hạn lượt đổi không hợp lệ");
        Reward reward = id == null ? new Reward() : rewards.findById(id).orElseThrow();
        reward.setName(name.trim()); reward.setDescription(description == null ? null : description.trim());
        reward.setPointsCost(pointsCost); reward.setVoucherAmount(voucherAmount);
        reward.setVoucherMinOrderAmount(minOrder == null ? BigDecimal.ZERO : minOrder);
        reward.setValidityDays(validityDays); reward.setRedemptionLimit(redemptionLimit); reward.setActive(active);
        reward.setUpdatedAt(LocalDateTime.now()); return rewards.save(reward);
    }

    private String normalizeRequestKey(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{16,80}")) throw new IllegalArgumentException("Mã yêu cầu đổi điểm không hợp lệ");
        return value;
    }

    private String newVoucherCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "RW" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
            if (!vouchers.existsByCodeIgnoreCase(code)) return code;
        }
        throw new IllegalStateException("Không thể tạo mã voucher duy nhất");
    }
}
