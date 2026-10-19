package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.LoyaltyLedgerRepository;
import com.rexchain.cinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LoyaltyService {
    private final UserRepository users;
    private final LoyaltyLedgerRepository ledger;
    private final MembershipService membership;

    public LoyaltyService(UserRepository users, LoyaltyLedgerRepository ledger, MembershipService membership) {
        this.users = users; this.ledger = ledger; this.membership = membership;
    }

    public int basePointsFor(BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.signum() <= 0) return 0;
        int points = paidAmount.divide(membership.settings().getAmountPerPoint(), 0, RoundingMode.DOWN).intValue();
        return points == 0 ? membership.settings().getMinimumPositiveOrderPoints() : points;
    }

    public int earnedPointsFor(BigDecimal paidAmount, BigDecimal multiplier) {
        int base = basePointsFor(paidAmount);
        if (base == 0) return 0;
        BigDecimal safeMultiplier = multiplier == null ? BigDecimal.ONE : multiplier;
        return BigDecimal.valueOf(base).multiply(safeMultiplier).setScale(0, RoundingMode.DOWN).intValue();
    }

    @Transactional
    public void awardBooking(Booking booking) {
        if (booking.isPointsAwarded() || booking.getStatus() != BookingStatus.CONFIRMED) return;
        String reference = "EARN:BOOKING:" + booking.getBookingCode();
        if (ledger.existsByReference(reference)) {
            booking.setPointsAwarded(true);
            return;
        }
        User user = users.findByIdForUpdate(booking.getUser().getId()).orElseThrow();
        MembershipTier tier = booking.getMembershipTierSnapshot() == null
                ? membership.tierFor(user.getTierQualifyingPoints()) : booking.getMembershipTierSnapshot();
        BigDecimal multiplier = booking.getPointMultiplier();
        if (multiplier == null || multiplier.signum() <= 0) multiplier = membership.policyFor(tier).getPointMultiplier();
        int points = earnedPointsFor(booking.getTotalAmount(), multiplier);
        booking.setMembershipTierSnapshot(tier); booking.setPointMultiplier(multiplier);
        booking.setEarnedPoints(points); booking.setPointsAwarded(true);
        apply(user, booking, null, LoyaltyTransactionType.EARN, LoyaltySource.BOOKING_PAYMENT, reference,
                points, points, points, 0, 0, "Cộng điểm booking đã thanh toán", "system");
    }

    @Transactional
    public void reverseBooking(Booking booking) {
        if (!booking.isPointsAwarded() || booking.isPointsReversed()) return;
        int earned = booking.getEarnedPoints();
        // Historic bookings without a stored snapshot are intentionally not recalculated.
        if (earned <= 0) return;
        String reference = "REVERSE:EARN:BOOKING:" + booking.getBookingCode();
        if (ledger.existsByReference(reference)) {
            booking.setPointsReversed(true);
            return;
        }
        User user = users.findByIdForUpdate(booking.getUser().getId()).orElseThrow();
        int availableDelta = -Math.min(earned, user.getLoyaltyPoints());
        int qualifyingDelta = -Math.min(earned, user.getTierQualifyingPoints());
        booking.setPointsReversed(true);
        apply(user, booking, null, LoyaltyTransactionType.REVERSE_EARN, LoyaltySource.BOOKING_REFUND, reference,
                availableDelta, qualifyingDelta, 0, 0, earned, "Hoàn tác điểm booking bị hủy/hoàn", "system");
    }

    @Transactional
    public void adjustAvailablePoints(Long userId, int delta, String reason, String actor) {
        if (delta == 0 || Math.abs(delta) > 100_000) throw new IllegalArgumentException("Số điểm điều chỉnh không hợp lệ");
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) throw new IllegalArgumentException("Lý do điều chỉnh là bắt buộc");
        User user = users.findByIdForUpdate(userId).orElseThrow();
        if (delta < 0 && user.getLoyaltyPoints() < -delta) throw new IllegalArgumentException("Điểm khả dụng không đủ để trừ");
        String reference = "ADMIN:ADJUST:" + userId + ":" + java.util.UUID.randomUUID();
        apply(user, null, null, LoyaltyTransactionType.ADMIN_ADJUSTMENT, LoyaltySource.ADMIN, reference,
                delta, 0, 0, 0, 0, reason.trim(), actor);
    }

    @Transactional
    public void redeem(User user, int points, RewardRedemption redemption, String reference) {
        User locked = users.findByIdForUpdate(user.getId()).orElseThrow();
        if (points <= 0 || locked.getLoyaltyPoints() < points) throw new IllegalArgumentException("Điểm khả dụng không đủ");
        apply(locked, null, redemption, LoyaltyTransactionType.REDEEM, LoyaltySource.REWARD_REDEMPTION, reference,
                -points, 0, 0, points, 0, "Đổi điểm lấy ưu đãi", "customer");
    }

    private void apply(User user, Booking booking, RewardRedemption redemption, LoyaltyTransactionType type,
                       LoyaltySource source, String reference, int availableDelta, int qualifyingDelta,
                       int lifetimeDelta, int redeemedDelta, int reversedDelta, String reason, String actor) {
        if (ledger.existsByReference(reference)) return;
        int availableBefore = user.getLoyaltyPoints(), qualifyingBefore = user.getTierQualifyingPoints();
        int lifetimeBefore = user.getLifetimePoints();
        user.setLoyaltyPoints(availableBefore + availableDelta);
        user.setTierQualifyingPoints(qualifyingBefore + qualifyingDelta);
        user.setLifetimePoints(lifetimeBefore + lifetimeDelta);
        user.setRedeemedPoints(user.getRedeemedPoints() + redeemedDelta);
        user.setReversedPoints(user.getReversedPoints() + reversedDelta);
        users.save(user);
        LoyaltyLedgerEntry entry = new LoyaltyLedgerEntry();
        entry.setUser(user); entry.setBooking(booking); entry.setRedemption(redemption);
        entry.setTransactionType(type); entry.setSource(source); entry.setReference(reference);
        entry.setPointsDelta(availableDelta); entry.setAvailableBefore(availableBefore); entry.setAvailableAfter(user.getLoyaltyPoints());
        entry.setQualifyingDelta(qualifyingDelta); entry.setQualifyingBefore(qualifyingBefore); entry.setQualifyingAfter(user.getTierQualifyingPoints());
        entry.setLifetimeDelta(lifetimeDelta); entry.setLifetimeBefore(lifetimeBefore); entry.setLifetimeAfter(user.getLifetimePoints());
        entry.setRedeemedDelta(redeemedDelta); entry.setReversedDelta(reversedDelta);
        entry.setReason(reason); entry.setActor(actor); ledger.save(entry);
    }
}
