-- Apply before deploying this version to production MySQL; dev uses Hibernate update.
ALTER TABLE app_users ADD COLUMN credential_version INT NOT NULL DEFAULT 0;
CREATE TABLE revoked_access_tokens (
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (token_hash),
    INDEX idx_revoked_access_tokens_expires_at (expires_at)
);
