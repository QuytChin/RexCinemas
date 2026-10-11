-- SQL Server V4 -> V5 non-destructive, repeatable migration.
-- This script runs BEFORE Hibernate schema update (SQL Server profile only).
-- No GO statements: compatible with Spring Boot SQL initializer.
-- Back up the database before first use.
IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL AND COL_LENGTH('dbo.combo_products', 'stock_quantity') IS NULL EXEC(N'ALTER TABLE dbo.combo_products ADD stock_quantity INT NOT NULL CONSTRAINT DF_rex_combo_stock DEFAULT (100)');
IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL AND COL_LENGTH('dbo.combo_products', 'type') IS NULL EXEC(N'ALTER TABLE dbo.combo_products ADD [type] VARCHAR(20) NOT NULL CONSTRAINT DF_rex_combo_type DEFAULT (''COMBO'')');
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL AND COL_LENGTH('dbo.movies', 'status') IS NULL EXEC(N'ALTER TABLE dbo.movies ADD [status] VARCHAR(20) NOT NULL CONSTRAINT DF_rex_movie_status DEFAULT (''NOW_SHOWING'')');
IF OBJECT_ID('dbo.showtimes', 'U') IS NOT NULL AND COL_LENGTH('dbo.showtimes', 'end_time') IS NULL EXEC(N'ALTER TABLE dbo.showtimes ADD end_time DATETIME2 NULL');
IF OBJECT_ID('dbo.booking_seats', 'U') IS NOT NULL AND COL_LENGTH('dbo.booking_seats', 'ticket_code') IS NULL EXEC(N'ALTER TABLE dbo.booking_seats ADD ticket_code VARCHAR(40) NULL');
IF OBJECT_ID('dbo.booking_seats', 'U') IS NOT NULL AND COL_LENGTH('dbo.booking_seats', 'qr_token') IS NULL EXEC(N'ALTER TABLE dbo.booking_seats ADD qr_token VARCHAR(80) NULL');
IF OBJECT_ID('dbo.booking_seats', 'U') IS NOT NULL AND COL_LENGTH('dbo.booking_seats', 'checked_in') IS NULL EXEC(N'ALTER TABLE dbo.booking_seats ADD checked_in BIT NOT NULL CONSTRAINT DF_rex_seat_checked DEFAULT (0)');
IF OBJECT_ID('dbo.booking_seats', 'U') IS NOT NULL AND COL_LENGTH('dbo.booking_seats', 'checked_in_at') IS NULL EXEC(N'ALTER TABLE dbo.booking_seats ADD checked_in_at DATETIME2 NULL');
IF OBJECT_ID('dbo.booking_seats', 'U') IS NOT NULL AND COL_LENGTH('dbo.booking_seats', 'checked_in_by') IS NULL EXEC(N'ALTER TABLE dbo.booking_seats ADD checked_in_by VARCHAR(180) NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'version') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD version BIGINT NOT NULL CONSTRAINT DF_rex_booking_version DEFAULT (0)');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'expires_at') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD expires_at DATETIME2 NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refunded_at') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refunded_at DATETIME2 NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refunded_by') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refunded_by VARCHAR(180) NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'refund_note') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD refund_note VARCHAR(255) NULL');
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL AND COL_LENGTH('dbo.bookings', 'points_awarded') IS NULL EXEC(N'ALTER TABLE dbo.bookings ADD points_awarded BIT NOT NULL CONSTRAINT DF_rex_points_awarded DEFAULT (0)');
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL AND COL_LENGTH('dbo.movies', 'age_rating') IS NOT NULL EXEC(N'UPDATE dbo.movies SET age_rating = ''P'' WHERE age_rating IS NULL OR UPPER(age_rating) NOT IN (''P'', ''K'', ''C13'', ''C16'', ''C18'', ''T13'', ''T16'', ''T18'')');
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL AND COL_LENGTH('dbo.movies', 'status') IS NOT NULL EXEC(N'UPDATE dbo.movies SET status = ''NOW_SHOWING'' WHERE status IS NULL');
IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL AND COL_LENGTH('dbo.combo_products', 'type') IS NOT NULL EXEC(N'UPDATE dbo.combo_products SET [type] = ''COMBO'' WHERE [type] IS NULL');
IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL AND COL_LENGTH('dbo.combo_products', 'stock_quantity') IS NOT NULL EXEC(N'UPDATE dbo.combo_products SET stock_quantity = 100 WHERE stock_quantity IS NULL');
