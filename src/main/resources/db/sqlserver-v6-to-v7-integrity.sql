IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND COL_LENGTH('dbo.app_users', 'version') IS NULL
    ALTER TABLE dbo.app_users ADD version BIGINT NOT NULL CONSTRAINT DF_rex_user_version DEFAULT (0);
IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL AND COL_LENGTH('dbo.combo_products', 'version') IS NULL
    ALTER TABLE dbo.combo_products ADD version BIGINT NOT NULL CONSTRAINT DF_rex_combo_version DEFAULT (0);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_booking_seats_ticket_code' AND object_id = OBJECT_ID('dbo.booking_seats'))
    CREATE UNIQUE INDEX uk_booking_seats_ticket_code ON dbo.booking_seats(ticket_code) WHERE ticket_code IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_seats_booking' AND object_id = OBJECT_ID('dbo.booking_seats'))
    CREATE INDEX idx_booking_seats_booking ON dbo.booking_seats(booking_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_combos_booking' AND object_id = OBJECT_ID('dbo.booking_combos'))
    CREATE INDEX idx_booking_combos_booking ON dbo.booking_combos(booking_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_booking_combos_combo' AND object_id = OBJECT_ID('dbo.booking_combos'))
    CREATE INDEX idx_booking_combos_combo ON dbo.booking_combos(combo_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_app_users_favorite_cinema' AND object_id = OBJECT_ID('dbo.app_users'))
    CREATE INDEX idx_app_users_favorite_cinema ON dbo.app_users(favorite_cinema_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_bookings_payment_reference' AND object_id = OBJECT_ID('dbo.bookings'))
    CREATE UNIQUE INDEX uk_bookings_payment_reference ON dbo.bookings(payment_method, payment_reference) WHERE payment_reference IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_auditoriums_cinema_name' AND object_id = OBJECT_ID('dbo.auditoriums'))
    CREATE UNIQUE INDEX uk_auditoriums_cinema_name ON dbo.auditoriums(cinema_id, name);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_cinemas_name_address' AND object_id = OBJECT_ID('dbo.cinemas'))
    CREATE UNIQUE INDEX uk_cinemas_name_address ON dbo.cinemas(name, address);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_movies_title_release_date' AND object_id = OBJECT_ID('dbo.movies'))
    CREATE UNIQUE INDEX uk_movies_title_release_date ON dbo.movies(title, release_date);

IF OBJECT_ID('dbo.showtimes', 'U') IS NOT NULL AND OBJECT_ID('dbo.movies', 'U') IS NOT NULL
    UPDATE s SET end_time = DATEADD(MINUTE, m.duration_minutes + 15, s.start_time)
    FROM dbo.showtimes s JOIN dbo.movies m ON m.id = s.movie_id WHERE s.end_time IS NULL;

IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_combo_products_stock_nonnegative')
    ALTER TABLE dbo.combo_products ADD CONSTRAINT ck_combo_products_stock_nonnegative CHECK (stock_quantity >= 0);
IF OBJECT_ID('dbo.vouchers', 'U') IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_vouchers_used_count_nonnegative')
    ALTER TABLE dbo.vouchers ADD CONSTRAINT ck_vouchers_used_count_nonnegative CHECK (used_count >= 0);
