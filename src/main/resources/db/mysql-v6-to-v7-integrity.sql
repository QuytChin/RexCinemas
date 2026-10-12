-- One-time MySQL 8 migration. Back up the database and run after mysql-v5-to-v6-registration.sql.
ALTER TABLE app_users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE combo_products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_booking_seats_ticket_code ON booking_seats(ticket_code);
CREATE INDEX idx_booking_seats_booking ON booking_seats(booking_id);
CREATE INDEX idx_booking_combos_booking ON booking_combos(booking_id);
CREATE INDEX idx_booking_combos_combo ON booking_combos(combo_id);
CREATE INDEX idx_app_users_favorite_cinema ON app_users(favorite_cinema_id);
CREATE UNIQUE INDEX uk_bookings_payment_reference ON bookings(payment_method, payment_reference);
CREATE UNIQUE INDEX uk_auditoriums_cinema_name ON auditoriums(cinema_id, name);
CREATE UNIQUE INDEX uk_cinemas_name_address ON cinemas(name, address);
CREATE UNIQUE INDEX uk_movies_title_release_date ON movies(title, release_date);

UPDATE showtimes s JOIN movies m ON m.id = s.movie_id
SET s.end_time = DATE_ADD(s.start_time, INTERVAL (m.duration_minutes + 15) MINUTE)
WHERE s.end_time IS NULL;

ALTER TABLE combo_products ADD CONSTRAINT ck_combo_products_stock_nonnegative CHECK (stock_quantity >= 0);
ALTER TABLE vouchers ADD CONSTRAINT ck_vouchers_used_count_nonnegative CHECK (used_count >= 0);
