-- Additive OTP challenge and rate-limit tables for MySQL.
CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    email VARCHAR(150) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    otp_salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE NULL,
    password_hash VARCHAR(255) NOT NULL,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    username VARCHAR(50) NULL,
    national_id VARCHAR(12) NULL,
    favorite_cinema_id BIGINT NULL,
    expires_at DATETIME(6) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    next_send_at DATETIME(6) NULL,
    client_ip VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_otp_challenge_email_purpose (email, purpose),
    KEY idx_otp_challenge_expires (expires_at),
    KEY idx_otp_challenge_locked (locked_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS otp_rate_limits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    scope_key VARCHAR(220) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    window_started_at DATETIME(6) NOT NULL,
    window_count INT NOT NULL DEFAULT 0,
    next_allowed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_otp_rate_scope_purpose (scope_key, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
