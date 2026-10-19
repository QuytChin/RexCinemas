-- Non-destructive PostgreSQL migration for manual refund reconciliation.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refund_reference VARCHAR(120) NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(12,2) NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refund_evidence VARCHAR(600) NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refunded_by_user_id BIGINT NULL;
