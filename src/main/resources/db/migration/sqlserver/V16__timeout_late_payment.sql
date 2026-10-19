-- Non-destructive SQL Server migration for immutable checkout holds and late-payment reconciliation.
IF OBJECT_ID('dbo.seat_hold_sessions', 'U') IS NULL
BEGIN
  CREATE TABLE dbo.seat_hold_sessions (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, version BIGINT NOT NULL CONSTRAINT df_hold_session_version DEFAULT 0,
    user_id BIGINT NOT NULL, showtime_id BIGINT NOT NULL, expires_at DATETIME2(3) NOT NULL, created_at DATETIME2(3) NOT NULL,
    CONSTRAINT uk_hold_session_user_showtime UNIQUE (user_id, showtime_id),
    CONSTRAINT fk_hold_session_user FOREIGN KEY (user_id) REFERENCES dbo.app_users(id),
    CONSTRAINT fk_hold_session_showtime FOREIGN KEY (showtime_id) REFERENCES dbo.showtimes(id)
  );
  CREATE INDEX idx_hold_session_expires ON dbo.seat_hold_sessions(expires_at);
END;
IF OBJECT_ID('dbo.late_payment_cases', 'U') IS NULL
BEGIN
  CREATE TABLE dbo.late_payment_cases (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, version BIGINT NOT NULL CONSTRAINT df_late_payment_case_version DEFAULT 0,
    booking_id BIGINT NOT NULL, provider_transaction_id VARCHAR(120) NOT NULL, payment_reference VARCHAR(120) NULL,
    amount DECIMAL(12,2) NOT NULL, bank_recorded_at DATETIME2(3) NULL, webhook_received_at DATETIME2(3) NULL,
    status VARCHAR(30) NOT NULL, reason VARCHAR(600) NULL, refunded_at DATETIME2(3) NULL, refunded_by VARCHAR(180) NULL,
    refunded_by_user_id BIGINT NULL, refund_reference VARCHAR(120) NULL, refund_amount DECIMAL(12,2) NULL,
    refund_evidence VARCHAR(600) NULL, refund_note VARCHAR(255) NULL, refund_failed_at DATETIME2(3) NULL,
    refund_failure_reason VARCHAR(255) NULL, created_at DATETIME2(3) NOT NULL, updated_at DATETIME2(3) NOT NULL,
    CONSTRAINT uk_late_payment_provider_transaction UNIQUE (provider_transaction_id),
    CONSTRAINT fk_late_payment_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(id)
  );
  CREATE INDEX idx_late_payment_status_created ON dbo.late_payment_cases(status, created_at);
  CREATE INDEX idx_late_payment_booking ON dbo.late_payment_cases(booking_id);
END;
IF OBJECT_ID('dbo.late_payment_case_audits', 'U') IS NULL
BEGIN
  CREATE TABLE dbo.late_payment_case_audits (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, late_payment_case_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL, actor VARCHAR(180) NULL, detail VARCHAR(600) NULL, created_at DATETIME2(3) NOT NULL,
    CONSTRAINT fk_late_payment_audit_case FOREIGN KEY (late_payment_case_id) REFERENCES dbo.late_payment_cases(id)
  );
  CREATE INDEX idx_late_payment_audit_case_created ON dbo.late_payment_case_audits(late_payment_case_id, created_at);
END;
