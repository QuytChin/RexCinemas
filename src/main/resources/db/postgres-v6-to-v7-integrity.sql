ALTER TABLE app_users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE combo_products ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_booking_seats_ticket_code
    ON booking_seats(ticket_code) WHERE ticket_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_booking_seats_booking ON booking_seats(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_combos_booking ON booking_combos(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_combos_combo ON booking_combos(combo_id);
CREATE INDEX IF NOT EXISTS idx_app_users_favorite_cinema ON app_users(favorite_cinema_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bookings_payment_reference
    ON bookings(payment_method, payment_reference) WHERE payment_reference IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_auditoriums_cinema_name_ci ON auditoriums(cinema_id, lower(name));
CREATE UNIQUE INDEX IF NOT EXISTS uk_cinemas_name_address_ci ON cinemas(lower(name), lower(address));
CREATE UNIQUE INDEX IF NOT EXISTS uk_movies_title_release_date_ci ON movies(lower(title), release_date);

UPDATE showtimes s SET end_time = s.start_time + ((m.duration_minutes + 15) * INTERVAL '1 minute')
FROM movies m WHERE s.movie_id = m.id AND s.end_time IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_combo_products_stock_nonnegative') THEN
        ALTER TABLE combo_products ADD CONSTRAINT ck_combo_products_stock_nonnegative CHECK (stock_quantity >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_vouchers_used_count_nonnegative') THEN
        ALTER TABLE vouchers ADD CONSTRAINT ck_vouchers_used_count_nonnegative CHECK (used_count >= 0);
    END IF;
END $$;
