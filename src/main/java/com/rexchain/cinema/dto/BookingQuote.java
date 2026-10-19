package com.rexchain.cinema.dto;

import com.rexchain.cinema.entity.MembershipTier;
import java.math.BigDecimal;

public record BookingQuote(BigDecimal seatAmount, BigDecimal comboAmount, BigDecimal voucherDiscount,
                           BigDecimal membershipDiscount, BigDecimal totalDiscount, BigDecimal totalAmount,
                           int expectedPoints, MembershipTier membershipTier, BigDecimal pointMultiplier,
                           boolean voucherApplied, String appliedVoucherCode) {}
