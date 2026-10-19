package com.rexchain.cinema.dto;

import com.rexchain.cinema.entity.MembershipTier;
import java.math.BigDecimal;

public record MembershipBenefitQuote(
        MembershipTier tier,
        BigDecimal ticketDiscountPercent,
        BigDecimal ticketDiscountAmount,
        BigDecimal voucherDiscountAmount,
        BigDecimal totalDiscountAmount,
        BigDecimal pointMultiplier,
        boolean voucherApplied,
        boolean membershipBenefitApplied
) {}
