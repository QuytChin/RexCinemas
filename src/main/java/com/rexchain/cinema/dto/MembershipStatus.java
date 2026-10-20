package com.rexchain.cinema.dto;

import com.rexchain.cinema.entity.MembershipTier;
import com.rexchain.cinema.entity.MembershipTierPolicy;

public record MembershipStatus(
        MembershipTier tier,
        MembershipTierPolicy policy,
        int qualifyingPoints,
        MembershipTier nextTier,
        Integer nextThreshold,
        int pointsToNext,
        int progressPercent) {
}
