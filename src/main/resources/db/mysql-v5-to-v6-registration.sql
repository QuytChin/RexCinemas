ALTER TABLE app_users
    ADD COLUMN username VARCHAR(50) NULL,
    ADD COLUMN birth_date DATE NULL,
    ADD COLUMN national_id VARCHAR(12) NULL,
    ADD COLUMN favorite_cinema_id BIGINT NULL,
    ADD UNIQUE KEY uk_app_users_username (username),
    ADD UNIQUE KEY uk_app_users_national_id (national_id),
    ADD CONSTRAINT fk_app_users_favorite_cinema FOREIGN KEY (favorite_cinema_id) REFERENCES cinemas(id);
