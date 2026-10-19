package com.rexchain.cinema.service;

public class OtpRateLimitException extends IllegalArgumentException {
    public OtpRateLimitException(String message) { super(message); }
}
