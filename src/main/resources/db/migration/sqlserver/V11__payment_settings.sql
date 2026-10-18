/*
  Phase 1 payment settings migration managed by Flyway.
  It is idempotent so an existing manually-created schema can be adopted
  safely when Flyway baselines the legacy database at version 10.
   No provider secret is stored here.
*/
IF OBJECT_ID('dbo.payment_settings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_settings (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment_settings PRIMARY KEY,
        payment_method VARCHAR(30) NOT NULL CONSTRAINT uk_payment_settings_method UNIQUE,
        enabled BIT NOT NULL CONSTRAINT df_payment_settings_enabled DEFAULT 0,
        bank_name NVARCHAR(120) NOT NULL CONSTRAINT df_payment_settings_bank_name DEFAULT N'',
        bank_code VARCHAR(20) NOT NULL CONSTRAINT df_payment_settings_bank_code DEFAULT '',
        bank_account VARCHAR(30) NOT NULL CONSTRAINT df_payment_settings_bank_account DEFAULT '',
        account_holder NVARCHAR(160) NOT NULL CONSTRAINT df_payment_settings_account_holder DEFAULT N'',
        default_account BIT NOT NULL CONSTRAINT df_payment_settings_default_account DEFAULT 1,
        payment_timeout_minutes INT NOT NULL CONSTRAINT df_payment_settings_timeout DEFAULT 5,
        payment_prefix VARCHAR(20) NOT NULL CONSTRAINT df_payment_settings_prefix DEFAULT 'REX',
        payment_description_template NVARCHAR(120) NOT NULL CONSTRAINT df_payment_settings_description DEFAULT N'{BOOKING_CODE}',
        version BIGINT NOT NULL CONSTRAINT df_payment_settings_version DEFAULT 0,
        updated_at DATETIME2(3) NOT NULL CONSTRAINT df_payment_settings_updated_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.payment_settings_audit', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_settings_audit (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment_settings_audit PRIMARY KEY,
        actor NVARCHAR(180) NOT NULL,
        action VARCHAR(30) NOT NULL,
        entity_key VARCHAR(60) NOT NULL,
        changed_fields NVARCHAR(1000) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT df_payment_settings_audit_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.payment_settings WHERE payment_method = 'BANK_QR')
    INSERT INTO dbo.payment_settings(payment_method) VALUES ('BANK_QR');
