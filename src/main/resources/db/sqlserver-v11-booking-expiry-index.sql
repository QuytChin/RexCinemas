-- Run once when upgrading an existing SQL Server database.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_bookings_status_expires_at' AND object_id = OBJECT_ID('bookings'))
    CREATE INDEX idx_bookings_status_expires_at ON bookings (status, expires_at);
