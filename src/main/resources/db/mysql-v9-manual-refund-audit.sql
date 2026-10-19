-- One-time MySQL migration for manual refund reconciliation. Back up before execution.
-- Hibernate ddl-auto=update creates the same nullable columns in local development.
ALTER TABLE bookings ADD COLUMN refund_reference VARCHAR(120) NULL;
ALTER TABLE bookings ADD COLUMN refund_amount DECIMAL(12,2) NULL;
ALTER TABLE bookings ADD COLUMN refund_evidence VARCHAR(600) NULL;
ALTER TABLE bookings ADD COLUMN refunded_by_user_id BIGINT NULL;
