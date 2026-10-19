-- Password reset OTP is isolated from registration OTP challenges.
CREATE TABLE IF NOT EXISTS password_reset_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    email VARCHAR(150) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    otp_salt VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    next_send_at DATETIME(6) NULL,
    otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at DATETIME(6) NULL,
    reset_token_hash VARCHAR(64) NULL,
    reset_token_expires_at DATETIME(6) NULL,
    client_ip VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_email_purpose (email, purpose),
    KEY idx_password_reset_expires (expires_at),
    KEY idx_password_reset_token_expires (reset_token_expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
