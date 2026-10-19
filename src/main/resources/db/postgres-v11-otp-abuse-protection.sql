-- Additive OTP challenge and rate-limit tables for PostgreSQL.
CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    email VARCHAR(150) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    otp_hash CHAR(64) NOT NULL,
    otp_salt CHAR(32) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE,
    password_hash VARCHAR(255) NOT NULL,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    username VARCHAR(50),
    national_id VARCHAR(12),
    favorite_cinema_id BIGINT,
    expires_at TIMESTAMP(3) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP(3),
    next_send_at TIMESTAMP(3),
    client_ip VARCHAR(64),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_otp_challenge_email_purpose UNIQUE (email, purpose)
);
CREATE INDEX IF NOT EXISTS idx_otp_challenge_expires ON otp_challenges(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_challenge_locked ON otp_challenges(locked_until);

CREATE TABLE IF NOT EXISTS otp_rate_limits (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    scope_key VARCHAR(220) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    window_started_at TIMESTAMP(3) NOT NULL,
    window_count INT NOT NULL DEFAULT 0,
    next_allowed_at TIMESTAMP(3),
    CONSTRAINT uk_otp_rate_scope_purpose UNIQUE (scope_key, purpose)
);
