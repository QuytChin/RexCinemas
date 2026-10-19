ALTER TABLE app_users ADD COLUMN credential_version INTEGER NOT NULL DEFAULT 0;
CREATE TABLE revoked_access_tokens (
    token_hash CHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_revoked_access_tokens_expires_at ON revoked_access_tokens (expires_at);
