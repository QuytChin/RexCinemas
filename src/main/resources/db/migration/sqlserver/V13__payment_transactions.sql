/*
  Phase 3 reconciliation ledger managed by Flyway.
  It is idempotent so an existing manually-created schema can be adopted
  safely when Flyway baselines the legacy database at version 10.
  No API key, webhook secret or raw authorization header is stored.
*/
IF OBJECT_ID('dbo.payment_transactions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_transactions (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment_transactions PRIMARY KEY,
        provider_transaction_id VARCHAR(120) NOT NULL CONSTRAINT uk_payment_transactions_provider UNIQUE,
        booking_code VARCHAR(30) NULL,
        payment_reference VARCHAR(120) NULL,
        gateway NVARCHAR(120) NULL,
        transaction_date VARCHAR(40) NULL,
        account_number_last4 VARCHAR(4) NULL,
        code VARCHAR(120) NULL,
        content NVARCHAR(500) NULL,
        transfer_type VARCHAR(20) NULL,
        amount DECIMAL(12,2) NULL,
        reference_code VARCHAR(120) NULL,
        status VARCHAR(30) NOT NULL,
        reason NVARCHAR(500) NULL,
        received_at DATETIME2(3) NOT NULL CONSTRAINT df_payment_transactions_received DEFAULT SYSUTCDATETIME(),
        verified_at DATETIME2(3) NULL,
        settled_at DATETIME2(3) NULL
    );
    CREATE INDEX ix_payment_transactions_status_received ON dbo.payment_transactions(status, received_at);
    CREATE INDEX ix_payment_transactions_booking ON dbo.payment_transactions(booking_code);
END;

IF OBJECT_ID('dbo.payment_transaction_audit', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_transaction_audit (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment_transaction_audit PRIMARY KEY,
        transaction_id BIGINT NOT NULL,
        action VARCHAR(40) NOT NULL,
        actor NVARCHAR(180) NOT NULL,
        details NVARCHAR(1000) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT df_payment_transaction_audit_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT fk_payment_transaction_audit_transaction FOREIGN KEY (transaction_id) REFERENCES dbo.payment_transactions(id)
    );
    CREATE INDEX ix_payment_transaction_audit_transaction ON dbo.payment_transaction_audit(transaction_id, created_at);
END;
