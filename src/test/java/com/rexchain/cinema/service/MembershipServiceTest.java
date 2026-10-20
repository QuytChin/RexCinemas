package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.LoyaltySettings;
import com.rexchain.cinema.entity.MembershipTier;
import com.rexchain.cinema.entity.MembershipTierPolicy;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.LoyaltyPolicyAuditRepository;
import com.rexchain.cinema.repository.LoyaltySettingsRepository;
import com.rexchain.cinema.repository.MembershipTierPolicyRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MembershipServiceTest {
    private final LoyaltySettingsRepository settings = mock(LoyaltySettingsRepository.class);
    private final MembershipTierPolicyRepository policies = mock(MembershipTierPolicyRepository.class);
    private final LoyaltyPolicyAuditRepository audits = mock(LoyaltyPolicyAuditRepository.class);

    @Test
    void usesPolicyBoundariesAndNeverReportsFullProgressBeforeNextTier() {
        usePolicies(standardPolicies(250, 600, 1200));
        MembershipService service = service();

        assertThat(service.statusFor(249).tier()).isEqualTo(MembershipTier.MEMBER);
        assertThat(service.statusFor(249).progressPercent()).isEqualTo(99);
        assertThat(service.statusFor(250).tier()).isEqualTo(MembershipTier.SILVER);
        assertThat(service.statusFor(599).tier()).isEqualTo(MembershipTier.SILVER);
        assertThat(service.statusFor(599).progressPercent()).isBetween(0, 99);
        assertThat(service.statusFor(600).tier()).isEqualTo(MembershipTier.GOLD);
        assertThat(service.statusFor(1199).tier()).isEqualTo(MembershipTier.GOLD);
        assertThat(service.statusFor(1200).tier()).isEqualTo(MembershipTier.DIAMOND);
        assertThat(service.statusFor(1200).progressPercent()).isEqualTo(100);
    }

    @Test
    void changedDatabaseThresholdIsSharedByTierProgressAndCheckoutBenefit() {
        usePolicies(standardPolicies(400, 700, 1300));
        when(settings.findById(1L)).thenReturn(Optional.of(settings()));
        MembershipService service = service();

        assertThat(service.statusFor(399).tier()).isEqualTo(MembershipTier.MEMBER);
        assertThat(service.statusFor(399).nextThreshold()).isEqualTo(400);
        assertThat(service.statusFor(400).tier()).isEqualTo(MembershipTier.SILVER);
        assertThat(service.statusFor(400).policy().getTicketDiscountPercent()).isEqualByComparingTo("7");
        assertThat(service.statusFor(400).policy().getPointMultiplier()).isEqualByComparingTo("1.1");

        User user = new User();
        user.setTierQualifyingPoints(400);
        var quote = service.quote(user, new BigDecimal("100000"), BigDecimal.ZERO);
        assertThat(quote.tier()).isEqualTo(MembershipTier.SILVER);
        assertThat(quote.ticketDiscountPercent()).isEqualByComparingTo("7");
        assertThat(quote.ticketDiscountAmount()).isEqualByComparingTo("7000");
    }

    @Test
    void disabledIntermediateTierIsSkippedAndDiamondDisabledIsAValidTopTierCase() {
        List<MembershipTierPolicy> configured = standardPolicies(250, 600, 1200);
        configured.get(1).setActive(false);
        configured.get(3).setActive(false);
        usePolicies(configured);
        MembershipService service = service();

        assertThat(service.statusFor(250).tier()).isEqualTo(MembershipTier.MEMBER);
        assertThat(service.statusFor(250).nextTier()).isEqualTo(MembershipTier.GOLD);
        assertThat(service.statusFor(600).tier()).isEqualTo(MembershipTier.GOLD);
        assertThat(service.statusFor(600).nextTier()).isNull();
        assertThat(service.statusFor(600).progressPercent()).isEqualTo(100);
    }

    @Test
    void missingOrInvalidPolicyFailsClearlyInsteadOfReturningHardcodedDefaults() {
        usePolicies(standardPolicies(250, 600, 1200).subList(0, 3));
        assertThatThrownBy(() -> service().statusFor(250))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("thiếu hoặc trùng");

        List<MembershipTierPolicy> duplicateBoundary = standardPolicies(250, 250, 1200);
        usePolicies(duplicateBoundary);
        assertThatThrownBy(() -> service().statusFor(250))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tăng dần");
    }

    @Test
    void explicitZeroQualifyingPointsDoesNotFallBackToLifetimePoints() {
        usePolicies(standardPolicies(250, 600, 1200));
        User user = new User();
        user.setTierQualifyingPoints(0);
        user.setLifetimePoints(1200);

        assertThat(service().statusFor(user.getTierQualifyingPoints()).tier()).isEqualTo(MembershipTier.MEMBER);
    }

    private MembershipService service() {
        return new MembershipService(settings, policies, audits);
    }

    private void usePolicies(List<MembershipTierPolicy> configured) {
        when(policies.findAllByOrderByQualifyingThresholdAsc()).thenReturn(configured);
    }

    private static List<MembershipTierPolicy> standardPolicies(int silver, int gold, int diamond) {
        return List.of(policy(MembershipTier.MEMBER, 0, "0", "1", true),
                policy(MembershipTier.SILVER, silver, "7", "1.1", true),
                policy(MembershipTier.GOLD, gold, "9", "1.2", true),
                policy(MembershipTier.DIAMOND, diamond, "12", "1.5", true));
    }

    private static MembershipTierPolicy policy(MembershipTier tier, int threshold, String discount,
                                               String multiplier, boolean active) {
        MembershipTierPolicy policy = new MembershipTierPolicy();
        policy.setTier(tier);
        policy.setQualifyingThreshold(threshold);
        policy.setTicketDiscountPercent(new BigDecimal(discount));
        policy.setPointMultiplier(new BigDecimal(multiplier));
        policy.setActive(active);
        return policy;
    }

    private static LoyaltySettings settings() {
        LoyaltySettings value = new LoyaltySettings();
        value.setId(1L);
        value.setAmountPerPoint(new BigDecimal("10000"));
        value.setMinimumPositiveOrderPoints(0);
        value.setVoucherStackingAllowed(true);
        value.setRedemptionEnabled(true);
        return value;
    }
}
