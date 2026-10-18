/*
  Phase 2 VietQR migration. Review and run manually; never add this file to
  spring.sql.init.schema-locations without DBA approval.
  Each booking gets an immutable bank/account snapshot. No secrets are stored.
*/
IF OBJECT_ID('dbo.bank_transfer_snapshots', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.bank_transfer_snapshots (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_bank_transfer_snapshots PRIMARY KEY,
        booking_code VARCHAR(30) NOT NULL CONSTRAINT uk_bank_transfer_snapshot_booking UNIQUE,
        bank_name NVARCHAR(120) NOT NULL CONSTRAINT df_bank_transfer_snapshot_bank_name DEFAULT N'',
        bank_code VARCHAR(20) NOT NULL,
        bank_account VARCHAR(30) NOT NULL,
        account_holder NVARCHAR(160) NOT NULL CONSTRAINT df_bank_transfer_snapshot_holder DEFAULT N'',
        amount DECIMAL(12,2) NOT NULL,
        payment_reference VARCHAR(120) NOT NULL CONSTRAINT uk_bank_transfer_snapshot_reference UNIQUE,
        expires_at DATETIME2(3) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT df_bank_transfer_snapshot_created DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX ix_bank_transfer_snapshot_expires ON dbo.bank_transfer_snapshots(expires_at);
END;
