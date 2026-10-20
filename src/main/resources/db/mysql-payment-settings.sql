CREATE TABLE IF NOT EXISTS payment_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_method VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    bank_name VARCHAR(120) NOT NULL DEFAULT '',
    bank_code VARCHAR(20) NOT NULL DEFAULT '',
    bank_account VARCHAR(30) NOT NULL DEFAULT '',
    account_holder VARCHAR(160) NOT NULL DEFAULT '',
    default_account BOOLEAN NOT NULL DEFAULT TRUE,
    payment_timeout_minutes INT NOT NULL DEFAULT 5,
    payment_prefix VARCHAR(20) NOT NULL DEFAULT 'REX',
    payment_description_template VARCHAR(120) NOT NULL DEFAULT '{BOOKING_CODE}',
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_settings_method (payment_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_settings_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor VARCHAR(180) NOT NULL,
    action VARCHAR(30) NOT NULL,
    entity_key VARCHAR(60) NOT NULL,
    changed_fields VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO payment_settings (payment_method)
VALUES ('BANK_QR')
ON DUPLICATE KEY UPDATE payment_method = VALUES(payment_method);

CREATE TABLE IF NOT EXISTS bank_transfer_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_code VARCHAR(30) NOT NULL,
    bank_name VARCHAR(120) NOT NULL DEFAULT '',
    bank_code VARCHAR(20) NOT NULL,
    bank_account VARCHAR(30) NOT NULL,
    account_holder VARCHAR(160) NOT NULL DEFAULT '',
    amount DECIMAL(12,2) NOT NULL,
    payment_reference VARCHAR(120) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_transfer_snapshot_booking (booking_code),
    UNIQUE KEY uk_bank_transfer_snapshot_reference (payment_reference),
    KEY ix_bank_transfer_snapshot_expires (expires_at),
    CONSTRAINT fk_bank_transfer_snapshot_booking
        FOREIGN KEY (booking_code) REFERENCES bookings(booking_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_transaction_id VARCHAR(120) NOT NULL,
    booking_code VARCHAR(30),
    payment_reference VARCHAR(120),
    gateway VARCHAR(120),
    transaction_date VARCHAR(40),
    account_number_last4 VARCHAR(4),
    code VARCHAR(120),
    content VARCHAR(500),
    transfer_type VARCHAR(20),
    amount DECIMAL(12,2),
    reference_code VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    received_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    verified_at TIMESTAMP(3) NULL,
    settled_at TIMESTAMP(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_transactions_provider (provider_transaction_id),
    KEY ix_payment_transactions_status_received (status, received_at),
    KEY ix_payment_transactions_booking (booking_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_transaction_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor VARCHAR(180) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY ix_payment_transaction_audit_transaction (transaction_id, created_at),
    CONSTRAINT fk_payment_transaction_audit_transaction
        FOREIGN KEY (transaction_id) REFERENCES payment_transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
