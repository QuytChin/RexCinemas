IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND COL_LENGTH('dbo.app_users', 'username') IS NULL
    ALTER TABLE dbo.app_users ADD username VARCHAR(50) NULL;
IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND COL_LENGTH('dbo.app_users', 'birth_date') IS NULL
    ALTER TABLE dbo.app_users ADD birth_date DATE NULL;
IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND COL_LENGTH('dbo.app_users', 'national_id') IS NULL
    ALTER TABLE dbo.app_users ADD national_id VARCHAR(12) NULL;
IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND COL_LENGTH('dbo.app_users', 'favorite_cinema_id') IS NULL
    ALTER TABLE dbo.app_users ADD favorite_cinema_id BIGINT NULL;

IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_app_users_username' AND object_id = OBJECT_ID('dbo.app_users'))
    CREATE UNIQUE INDEX uk_app_users_username ON dbo.app_users(username) WHERE username IS NOT NULL;
IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_app_users_national_id' AND object_id = OBJECT_ID('dbo.app_users'))
    CREATE UNIQUE INDEX uk_app_users_national_id ON dbo.app_users(national_id) WHERE national_id IS NOT NULL;
IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL AND OBJECT_ID('dbo.cinemas', 'U') IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_app_users_favorite_cinema')
    ALTER TABLE dbo.app_users ADD CONSTRAINT fk_app_users_favorite_cinema
        FOREIGN KEY (favorite_cinema_id) REFERENCES dbo.cinemas(id);
