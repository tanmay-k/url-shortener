CREATE TABLE revoked_token (
    jti VARCHAR(36) NOT NULL PRIMARY KEY,
    expires_at DATETIME NOT NULL
);

CREATE INDEX revoked_token_expires_at_IDX ON revoked_token (expires_at);
