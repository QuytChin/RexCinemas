package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.LoyaltySettings;
import com.rexchain.cinema.repository.LoyaltyLedgerRepository;
import com.rexchain.cinema.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {
    @Mock UserRepository users;
    @Mock LoyaltyLedgerRepository ledger;
    @Mock MembershipService membership;

    @Test
    void preservesBaseRuleMinimumOnePointAndDownRounding() {
        LoyaltySettings settings = new LoyaltySettings();
        settings.setAmountPerPoint(BigDecimal.valueOf(10_000));
        settings.setMinimumPositiveOrderPoints(1);
        when(membership.settings()).thenReturn(settings);
        LoyaltyService service = new LoyaltyService(users, ledger, membership);

        assertThat(service.basePointsFor(BigDecimal.ZERO)).isZero();
        assertThat(service.basePointsFor(BigDecimal.valueOf(9_999))).isEqualTo(1);
        assertThat(service.basePointsFor(BigDecimal.valueOf(10_000))).isEqualTo(1);
        assertThat(service.basePointsFor(BigDecimal.valueOf(29_999))).isEqualTo(2);
    }

    @Test
    void appliesTierMultiplierAfterBasePointRounding() {
        LoyaltySettings settings = new LoyaltySettings();
        settings.setAmountPerPoint(BigDecimal.valueOf(10_000));
        settings.setMinimumPositiveOrderPoints(1);
        when(membership.settings()).thenReturn(settings);
        LoyaltyService service = new LoyaltyService(users, ledger, membership);

        assertThat(service.earnedPointsFor(BigDecimal.valueOf(29_999), new BigDecimal("1.5"))).isEqualTo(3);
        assertThat(service.earnedPointsFor(BigDecimal.valueOf(9_999), new BigDecimal("1.1"))).isEqualTo(1);
    }
}
