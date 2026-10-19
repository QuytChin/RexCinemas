package com.rexchain.cinema.entity;
/**
 * Legacy values remain readable for historical bookings; new bookings accept BANK_QR only.
 */
public enum PaymentMethod { PAY_AT_COUNTER, MOMO_SANDBOX, VNPAY_SANDBOX, BANK_QR }
