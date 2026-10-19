-- Manual refund reconciliation audit fields; no money movement is initiated by this migration.
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refund_reference') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refund_reference VARCHAR(120) NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refund_amount') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refund_amount DECIMAL(12,2) NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refund_evidence') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refund_evidence VARCHAR(600) NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refunded_by_user_id') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refunded_by_user_id BIGINT NULL');
