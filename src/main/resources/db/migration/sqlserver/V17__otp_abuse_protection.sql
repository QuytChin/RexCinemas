-- Additive OTP challenge and rate-limit tables for SQL Server.
IF OBJECT_ID('dbo.otp_challenges', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.otp_challenges (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_otp_challenges PRIMARY KEY,
        version BIGINT NOT NULL CONSTRAINT df_otp_challenge_version DEFAULT 0,
        email VARCHAR(150) NOT NULL,
        purpose VARCHAR(40) NOT NULL,
        otp_hash CHAR(64) NOT NULL,
        otp_salt CHAR(32) NOT NULL,
        full_name NVARCHAR(120) NOT NULL,
        phone VARCHAR(30) NOT NULL,
        gender NVARCHAR(20) NOT NULL,
        birth_date DATE NULL,
        password_hash VARCHAR(255) NOT NULL,
        terms_accepted BIT NOT NULL CONSTRAINT df_otp_challenge_terms DEFAULT 0,
        username VARCHAR(50) NULL,
        national_id VARCHAR(12) NULL,
        favorite_cinema_id BIGINT NULL,
        expires_at DATETIME2(3) NOT NULL,
        failed_attempts INT NOT NULL CONSTRAINT df_otp_challenge_failed DEFAULT 0,
        locked_until DATETIME2(3) NULL,
        next_send_at DATETIME2(3) NULL,
        client_ip VARCHAR(64) NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT df_otp_challenge_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT df_otp_challenge_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uk_otp_challenge_email_purpose UNIQUE (email, purpose)
    );
    CREATE INDEX idx_otp_challenge_expires ON dbo.otp_challenges(expires_at);
    CREATE INDEX idx_otp_challenge_locked ON dbo.otp_challenges(locked_until);
END;

IF OBJECT_ID('dbo.otp_rate_limits', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.otp_rate_limits (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_otp_rate_limits PRIMARY KEY,
        version BIGINT NOT NULL CONSTRAINT df_otp_rate_version DEFAULT 0,
        scope_key VARCHAR(220) NOT NULL,
        purpose VARCHAR(40) NOT NULL,
        window_started_at DATETIME2(3) NOT NULL,
        window_count INT NOT NULL CONSTRAINT df_otp_rate_count DEFAULT 0,
        next_allowed_at DATETIME2(3) NULL,
        CONSTRAINT uk_otp_rate_scope_purpose UNIQUE (scope_key, purpose)
    );
END;
