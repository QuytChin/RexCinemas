package com.rexchain.cinema.entity;

/** A verified bank payment that cannot safely produce a ticket. */
public enum LatePaymentCaseStatus { PENDING_REFUND, NEEDS_REVIEW, REFUND_FAILED, REFUNDED }
