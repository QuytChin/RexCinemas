package com.rexchain.cinema.service;

public class LoginRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginRateLimitException(long retryAfterSeconds) {
        super("Đăng nhập tạm thời bị giới hạn. Vui lòng thử lại sau.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
