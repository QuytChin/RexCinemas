-- Add uniqueness protection only when legacy data is already unambiguous.
-- Existing data is never changed or deleted by this repeatable migration.
IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.app_users', 'username') IS NOT NULL
   AND NOT EXISTS (
       SELECT username FROM dbo.app_users
       WHERE username IS NOT NULL AND username <> ''
       GROUP BY username HAVING COUNT(*) > 1
   )
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.app_users') AND name = 'ux_app_users_username')
    CREATE UNIQUE INDEX ux_app_users_username ON dbo.app_users(username) WHERE username IS NOT NULL AND username <> '';

IF OBJECT_ID('dbo.app_users', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.app_users', 'phone') IS NOT NULL
   AND NOT EXISTS (
       SELECT phone FROM dbo.app_users
       WHERE phone IS NOT NULL AND phone <> ''
       GROUP BY phone HAVING COUNT(*) > 1
   )
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.app_users') AND name = 'ux_app_users_phone')
    CREATE UNIQUE INDEX ux_app_users_phone ON dbo.app_users(phone) WHERE phone IS NOT NULL AND phone <> '';
