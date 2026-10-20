package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.MembershipBenefitQuote;
import com.rexchain.cinema.dto.MembershipStatus;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.LoyaltyPolicyAuditRepository;
import com.rexchain.cinema.repository.LoyaltySettingsRepository;
import com.rexchain.cinema.repository.MembershipTierPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MembershipService {
    private static final long TIER_POLICY_CACHE_MILLIS = 30_000;
    private final LoyaltySettingsRepository settingsRepository;
    private final MembershipTierPolicyRepository policies;
    private final LoyaltyPolicyAuditRepository policyAudits;
    private volatile List<MembershipTierPolicy> cachedTierPolicies;
    private volatile long tierPolicyCacheExpiresAt;

    public MembershipService(LoyaltySettingsRepository settingsRepository, MembershipTierPolicyRepository policies,
                             LoyaltyPolicyAuditRepository policyAudits) {
        this.settingsRepository = settingsRepository;
        this.policies = policies;
        this.policyAudits = policyAudits;
    }

    @Transactional(readOnly = true)
    public LoyaltySettings settings() {
        return settingsRepository.findById(1L).orElseGet(this::defaultSettings);
    }

    @Transactional(readOnly = true)
    public List<MembershipTierPolicy> tierPolicies() {
        List<MembershipTierPolicy> cached = cachedTierPolicies;
        long now = System.currentTimeMillis();
        if (cached != null && now < tierPolicyCacheExpiresAt) return cached;
        synchronized (this) {
            cached = cachedTierPolicies;
            now = System.currentTimeMillis();
            if (cached == null || now >= tierPolicyCacheExpiresAt) {
                List<MembershipTierPolicy> configured = policies.findAllByOrderByQualifyingThresholdAsc();
                validatePolicies(configured);
                cached = List.copyOf(configured);
                cachedTierPolicies = cached;
                tierPolicyCacheExpiresAt = now + TIER_POLICY_CACHE_MILLIS;
            }
            return cached;
        }
    }

    @Transactional(readOnly = true)
    public MembershipTierPolicy policyFor(MembershipTier tier) {
        return tierPolicies().stream().filter(value -> value.getTier() == tier && value.isActive()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Chính sách hạng " + tier + " đang thiếu hoặc bị tắt"));
    }

    @Transactional(readOnly = true)
    public MembershipTier tierFor(int qualifyingPoints) {
        return statusFor(qualifyingPoints).tier();
    }

    @Transactional(readOnly = true)
    public int nextThreshold(int qualifyingPoints) {
        MembershipStatus status = statusFor(qualifyingPoints);
        return status.nextThreshold() == null ? qualifyingPoints : status.nextThreshold();
    }

    @Transactional(readOnly = true)
    public int progressPercent(int qualifyingPoints) {
        return statusFor(qualifyingPoints).progressPercent();
    }

    @Transactional(readOnly = true)
    public MembershipStatus statusFor(int qualifyingPoints) {
        List<MembershipTierPolicy> active = tierPolicies().stream().filter(MembershipTierPolicy::isActive)
                .sorted(Comparator.comparingInt(policy -> policy.getTier().ordinal())).toList();
        MembershipTierPolicy current = active.stream()
                .filter(policy -> policy.getQualifyingThreshold() <= qualifyingPoints)
                .max(Comparator.comparingInt(policy -> policy.getTier().ordinal()))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy hạng áp dụng cho số điểm xét hạng"));
        MembershipTierPolicy next = active.stream()
                .filter(policy -> policy.getTier().ordinal() > current.getTier().ordinal()).findFirst().orElse(null);
        if (next == null) {
            return new MembershipStatus(current.getTier(), current, qualifyingPoints, null, null, 0, 100);
        }
        int start = current.getQualifyingThreshold();
        int target = next.getQualifyingThreshold();
        int progress = target <= start ? 0 : (int) Math.floor(
                Math.max(0L, (long) qualifyingPoints - start) * 100.0 / (target - start));
        progress = Math.min(99, Math.max(0, progress));
        return new MembershipStatus(current.getTier(), current, qualifyingPoints, next.getTier(), target,
                Math.max(0, target - qualifyingPoints), progress);
    }

    @Transactional(readOnly = true)
    public MembershipBenefitQuote quote(User user, BigDecimal seatAmount, BigDecimal voucherDiscount) {
        MembershipTier tier = tierFor(user.getTierQualifyingPoints());
        MembershipTierPolicy policy = policyFor(tier);
        BigDecimal safeSeat = positive(seatAmount);
        BigDecimal voucher = positive(voucherDiscount);
        BigDecimal tierDiscount = safeSeat.multiply(policy.getTicketDiscountPercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        boolean stack = settings().isVoucherStackingAllowed();
        boolean voucherApplied = voucher.signum() > 0 && (stack || voucher.compareTo(tierDiscount) >= 0);
        boolean membershipApplied = tierDiscount.signum() > 0 && (stack || tierDiscount.compareTo(voucher) > 0);
        BigDecimal appliedVoucher = voucherApplied ? voucher : BigDecimal.ZERO;
        BigDecimal appliedTier = membershipApplied ? tierDiscount : BigDecimal.ZERO;
        return new MembershipBenefitQuote(tier, policy.getTicketDiscountPercent(), appliedTier, appliedVoucher,
                appliedTier.add(appliedVoucher), policy.getPointMultiplier(), voucherApplied, membershipApplied);
    }

    @Transactional
    public void saveSettings(BigDecimal amountPerPoint, int minimumPositivePoints, boolean stacking,
                             boolean redemptionEnabled, String actor) {
        if (amountPerPoint == null || amountPerPoint.compareTo(BigDecimal.ONE) < 0
                || amountPerPoint.compareTo(BigDecimal.valueOf(10_000_000)) > 0)
            throw new IllegalArgumentException("Số tiền quy đổi điểm không hợp lệ");
        if (minimumPositivePoints < 0 || minimumPositivePoints > 100)
            throw new IllegalArgumentException("Điểm tối thiểu không hợp lệ");
        LoyaltySettings value = settingsRepository.findById(1L).orElseGet(this::defaultSettings);
        String before = settingsSnapshot(value);
        value.setAmountPerPoint(amountPerPoint.setScale(2, RoundingMode.DOWN));
        value.setMinimumPositiveOrderPoints(minimumPositivePoints);
        value.setVoucherStackingAllowed(stacking);
        value.setRedemptionEnabled(redemptionEnabled);
        value.setUpdatedAt(LocalDateTime.now()); value.setUpdatedBy(actor);
        settingsRepository.save(value);
        audit("SETTINGS", "global", before, settingsSnapshot(value), actor);
    }

    @Transactional
    public void saveTierPolicy(MembershipTier tier, int threshold, BigDecimal ticketDiscountPercent,
                               BigDecimal pointMultiplier, boolean active, String actor) {
        if (threshold < 0) throw new IllegalArgumentException("Ngưỡng điểm không hợp lệ");
        if (ticketDiscountPercent == null || ticketDiscountPercent.compareTo(BigDecimal.ZERO) < 0
                || ticketDiscountPercent.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Giảm giá phải từ 0 đến 100%");
        if (pointMultiplier == null || pointMultiplier.compareTo(BigDecimal.ONE) < 0
                || pointMultiplier.compareTo(BigDecimal.TEN) > 0)
            throw new IllegalArgumentException("Hệ số điểm phải từ 1 đến 10");
        MembershipTierPolicy value = policies.findByTier(tier).orElseGet(MembershipTierPolicy::new);
        String before = policySnapshot(value);
        value.setTier(tier); value.setQualifyingThreshold(threshold);
        value.setTicketDiscountPercent(ticketDiscountPercent.setScale(4, RoundingMode.DOWN));
        value.setPointMultiplier(pointMultiplier.setScale(4, RoundingMode.DOWN));
        value.setActive(active); value.setUpdatedAt(LocalDateTime.now()); value.setUpdatedBy(actor);
        policies.save(value);
        validateTierOrder();
        audit("TIER", tier.name(), before, policySnapshot(value), actor);
        cachedTierPolicies = null;
        tierPolicyCacheExpiresAt = 0;
    }

    private void validateTierOrder() {
        validatePolicies(policies.findAllByOrderByQualifyingThresholdAsc());
    }

    private void validatePolicies(List<MembershipTierPolicy> values) {
        if (values.size() != MembershipTier.values().length
                || values.stream().map(MembershipTierPolicy::getTier).filter(Objects::nonNull).distinct().count()
                != MembershipTier.values().length) {
            throw new IllegalStateException("Cấu hình hạng thành viên trong database đang thiếu hoặc trùng");
        }
        List<MembershipTierPolicy> byRank = values.stream()
                .sorted(Comparator.comparingInt(value -> value.getTier().ordinal())).toList();
        MembershipTierPolicy member = byRank.getFirst();
        if (member.getTier() != MembershipTier.MEMBER || !member.isActive() || member.getQualifyingThreshold() != 0) {
            throw new IllegalStateException("Chính sách MEMBER phải hoạt động và có ngưỡng 0 điểm");
        }
        int previous = -1;
        for (MembershipTierPolicy policy : byRank) {
            if (policy.getQualifyingThreshold() < 0 || policy.getQualifyingThreshold() <= previous)
                throw new IllegalStateException("Ngưỡng hạng thành viên phải tăng dần và không trùng nhau");
            if (policy.getTicketDiscountPercent() == null || policy.getTicketDiscountPercent().signum() < 0
                    || policy.getTicketDiscountPercent().compareTo(BigDecimal.valueOf(100)) > 0
                    || policy.getPointMultiplier() == null || policy.getPointMultiplier().signum() <= 0)
                throw new IllegalStateException("Quyền lợi trong chính sách hạng thành viên không hợp lệ");
            previous = policy.getQualifyingThreshold();
        }
    }

    private LoyaltySettings defaultSettings() {
        LoyaltySettings value = new LoyaltySettings();
        value.setId(1L); value.setAmountPerPoint(BigDecimal.valueOf(10_000));
        value.setMinimumPositiveOrderPoints(1); value.setVoucherStackingAllowed(false); value.setRedemptionEnabled(true);
        return value;
    }

    private void audit(String type, String key, String before, String after, String actor) {
        LoyaltyPolicyAudit audit = new LoyaltyPolicyAudit();
        audit.setPolicyType(type); audit.setPolicyKey(key); audit.setBeforeValue(before); audit.setAfterValue(after);
        audit.setActor(actor == null || actor.isBlank() ? "system" : actor); audit.setCreatedAt(LocalDateTime.now());
        policyAudits.save(audit);
    }

    private String settingsSnapshot(LoyaltySettings value) {
        return "amountPerPoint=" + value.getAmountPerPoint() + ";minimumPositiveOrderPoints="
                + value.getMinimumPositiveOrderPoints() + ";voucherStackingAllowed=" + value.isVoucherStackingAllowed()
                + ";redemptionEnabled=" + value.isRedemptionEnabled();
    }

    private String policySnapshot(MembershipTierPolicy value) {
        return "tier=" + value.getTier() + ";threshold=" + value.getQualifyingThreshold() + ";ticketDiscount="
                + value.getTicketDiscountPercent() + ";pointMultiplier=" + value.getPointMultiplier() + ";active="
                + value.isActive();
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
