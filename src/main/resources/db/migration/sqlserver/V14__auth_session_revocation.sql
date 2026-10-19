ALTER TABLE app_users ADD credential_version INT NOT NULL CONSTRAINT DF_app_users_credential_version DEFAULT 0;
CREATE TABLE revoked_access_tokens (
    token_hash CHAR(64) NOT NULL PRIMARY KEY,
    expires_at DATETIME2(6) NOT NULL
);
CREATE INDEX idx_revoked_access_tokens_expires_at ON revoked_access_tokens (expires_at);
