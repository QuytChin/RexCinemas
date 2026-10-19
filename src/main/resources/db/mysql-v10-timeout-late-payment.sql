-- One-time non-destructive migration. Review on a backup before production use.
CREATE TABLE IF NOT EXISTS seat_hold_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL, showtime_id BIGINT NOT NULL, expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_hold_session_user_showtime (user_id, showtime_id), KEY idx_hold_session_expires (expires_at),
  CONSTRAINT fk_hold_session_user FOREIGN KEY (user_id) REFERENCES app_users(id),
  CONSTRAINT fk_hold_session_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS late_payment_cases (
  id BIGINT NOT NULL AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, booking_id BIGINT NOT NULL,
  provider_transaction_id VARCHAR(120) NOT NULL, payment_reference VARCHAR(120) NULL,
  amount DECIMAL(12,2) NOT NULL, bank_recorded_at DATETIME(3) NULL, webhook_received_at DATETIME(3) NULL,
  status VARCHAR(30) NOT NULL, reason VARCHAR(600) NULL, refunded_at DATETIME(3) NULL,
  refunded_by VARCHAR(180) NULL, refunded_by_user_id BIGINT NULL, refund_reference VARCHAR(120) NULL,
  refund_amount DECIMAL(12,2) NULL, refund_evidence VARCHAR(600) NULL, refund_note VARCHAR(255) NULL,
  refund_failed_at DATETIME(3) NULL, refund_failure_reason VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_late_payment_provider_transaction (provider_transaction_id),
  KEY idx_late_payment_status_created (status, created_at), KEY idx_late_payment_booking (booking_id),
  CONSTRAINT fk_late_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS late_payment_case_audits (
  id BIGINT NOT NULL AUTO_INCREMENT, late_payment_case_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL, actor VARCHAR(180) NULL, detail VARCHAR(600) NULL,
  created_at DATETIME(3) NOT NULL, PRIMARY KEY (id),
  KEY idx_late_payment_audit_case_created (late_payment_case_id, created_at),
  CONSTRAINT fk_late_payment_audit_case FOREIGN KEY (late_payment_case_id) REFERENCES late_payment_cases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Hibernate may have created the tables before this migration was applied. Add the
-- missing hold-session user foreign key without touching existing rows.
SET @hold_user_fk_exists = (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE constraint_schema = DATABASE() AND table_name = 'seat_hold_sessions'
    AND column_name = 'user_id' AND referenced_table_name = 'app_users'
);
SET @hold_user_fk_sql = IF(@hold_user_fk_exists = 0,
  'ALTER TABLE seat_hold_sessions ADD CONSTRAINT fk_hold_session_user FOREIGN KEY (user_id) REFERENCES app_users(id)',
  'SELECT 1');
PREPARE hold_user_fk_stmt FROM @hold_user_fk_sql;
EXECUTE hold_user_fk_stmt;
DEALLOCATE PREPARE hold_user_fk_stmt;
