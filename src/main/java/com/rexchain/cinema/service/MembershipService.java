package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.MembershipBenefitQuote;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.LoyaltySettingsRepository;
import com.rexchain.cinema.repository.LoyaltyPolicyAuditRepository;
import com.rexchain.cinema.repository.MembershipTierPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MembershipService {
    private final LoyaltySettingsRepository settingsRepository;
    private final MembershipTierPolicyRepository policies;
    private final LoyaltyPolicyAuditRepository policyAudits;

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
        List<MembershipTierPolicy> configured = policies.findAllByOrderByQualifyingThresholdAsc();
        return configured.size() == MembershipTier.values().length ? configured : defaults();
    }

    @Transactional(readOnly = true)
    public MembershipTierPolicy policyFor(MembershipTier tier) {
        return tierPolicies().stream().filter(value -> value.getTier() == tier).findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu cấu hình hạng " + tier));
    }

    @Transactional(readOnly = true)
    public MembershipTier tierFor(int qualifyingPoints) {
        MembershipTier result = MembershipTier.MEMBER;
        for (MembershipTierPolicy policy : tierPolicies()) {
            if (policy.isActive() && qualifyingPoints >= policy.getQualifyingThreshold()) result = policy.getTier();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public int nextThreshold(int qualifyingPoints) {
        return tierPolicies().stream().filter(policy -> policy.isActive() && policy.getQualifyingThreshold() > qualifyingPoints)
                .mapToInt(MembershipTierPolicy::getQualifyingThreshold).min().orElse(qualifyingPoints);
    }

    @Transactional(readOnly = true)
    public int progressPercent(int qualifyingPoints) {
        List<MembershipTierPolicy> values = tierPolicies();
        int start = 0;
        int target = 0;
        for (MembershipTierPolicy policy : values) {
            if (!policy.isActive()) continue;
            if (qualifyingPoints < policy.getQualifyingThreshold()) {
                target = policy.getQualifyingThreshold();
                break;
            }
            start = policy.getQualifyingThreshold();
        }
        if (target == 0) return 100;
        return Math.min(100, Math.max(0, (int) Math.round((qualifyingPoints - start) * 100.0 / (target - start))));
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
    public void saveSettings(BigDecimal amountPerPoint, int minimumPositivePoints, boolean stacking, boolean redemptionEnabled, String actor) {
        if (amountPerPoint == null || amountPerPoint.compareTo(BigDecimal.ONE) < 0 || amountPerPoint.compareTo(BigDecimal.valueOf(10_000_000)) > 0)
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
        if (ticketDiscountPercent == null || ticketDiscountPercent.compareTo(BigDecimal.ZERO) < 0 || ticketDiscountPercent.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Giảm giá phải từ 0 đến 100%");
        if (pointMultiplier == null || pointMultiplier.compareTo(BigDecimal.ONE) < 0 || pointMultiplier.compareTo(BigDecimal.TEN) > 0)
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
    }

    private void validateTierOrder() {
        List<MembershipTierPolicy> values = policies.findAllByOrderByQualifyingThresholdAsc();
        if (values.size() != MembershipTier.values().length) return;
        int previous = -1;
        for (MembershipTierPolicy policy : values) {
            if (policy.getQualifyingThreshold() <= previous)
                throw new IllegalArgumentException("Ngưỡng các hạng phải tăng dần và không trùng nhau");
            previous = policy.getQualifyingThreshold();
        }
        if (values.getFirst().getTier() != MembershipTier.MEMBER || values.getFirst().getQualifyingThreshold() != 0)
            throw new IllegalArgumentException("Hạng MEMBER phải bắt đầu từ 0 điểm");
    }

    private LoyaltySettings defaultSettings() {
        LoyaltySettings value = new LoyaltySettings();
        value.setId(1L); value.setAmountPerPoint(BigDecimal.valueOf(10_000));
        value.setMinimumPositiveOrderPoints(1); value.setVoucherStackingAllowed(false); value.setRedemptionEnabled(true);
        return value;
    }

    private List<MembershipTierPolicy> defaults() {
        return List.of(policy(MembershipTier.MEMBER, 0, "0", "1.0"), policy(MembershipTier.SILVER, 250, "3", "1.1"),
                policy(MembershipTier.GOLD, 600, "5", "1.2"), policy(MembershipTier.DIAMOND, 1200, "8", "1.5"));
    }

    private MembershipTierPolicy policy(MembershipTier tier, int threshold, String discount, String multiplier) {
        MembershipTierPolicy value = new MembershipTierPolicy();
        value.setTier(tier); value.setQualifyingThreshold(threshold);
        value.setTicketDiscountPercent(new BigDecimal(discount)); value.setPointMultiplier(new BigDecimal(multiplier));
        value.setActive(true); return value;
    }

    private void audit(String type, String key, String before, String after, String actor) {
        LoyaltyPolicyAudit audit = new LoyaltyPolicyAudit();
        audit.setPolicyType(type); audit.setPolicyKey(key); audit.setBeforeValue(before); audit.setAfterValue(after);
        audit.setActor(actor == null || actor.isBlank() ? "system" : actor); audit.setCreatedAt(LocalDateTime.now());
        policyAudits.save(audit);
    }

    private String settingsSnapshot(LoyaltySettings value) {
        return "amountPerPoint=" + value.getAmountPerPoint() + ";minimumPositiveOrderPoints=" + value.getMinimumPositiveOrderPoints()
                + ";voucherStackingAllowed=" + value.isVoucherStackingAllowed() + ";redemptionEnabled=" + value.isRedemptionEnabled();
    }

    private String policySnapshot(MembershipTierPolicy value) {
        return "tier=" + value.getTier() + ";threshold=" + value.getQualifyingThreshold() + ";ticketDiscount="
                + value.getTicketDiscountPercent() + ";pointMultiplier=" + value.getPointMultiplier() + ";active=" + value.isActive();
    }

    private BigDecimal positive(BigDecimal value) { return value == null || value.signum() < 0 ? BigDecimal.ZERO : value; }
}
