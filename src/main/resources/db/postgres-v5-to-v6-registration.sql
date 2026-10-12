ALTER TABLE app_users ADD COLUMN IF NOT EXISTS username VARCHAR(50);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS national_id VARCHAR(12);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS favorite_cinema_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_users_username ON app_users(username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_app_users_national_id ON app_users(national_id) WHERE national_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_app_users_favorite_cinema') THEN
        ALTER TABLE app_users ADD CONSTRAINT fk_app_users_favorite_cinema
            FOREIGN KEY (favorite_cinema_id) REFERENCES cinemas(id);
    END IF;
END $$;
