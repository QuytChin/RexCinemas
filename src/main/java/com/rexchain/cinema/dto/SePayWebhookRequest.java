package com.rexchain.cinema.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SePayWebhookRequest(
        String id,
        String gateway,
        String transactionDate,
        String accountNumber,
        String subAccount,
        String code,
        String content,
        String transferType,
        BigDecimal transferAmount,
        String referenceCode
) {}
