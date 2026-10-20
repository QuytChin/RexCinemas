-- Run once when upgrading an existing PostgreSQL database.
CREATE INDEX IF NOT EXISTS idx_bookings_status_expires_at ON bookings (status, expires_at);
