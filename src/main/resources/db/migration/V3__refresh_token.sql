CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id VARCHAR(36) NOT NULL,
    user_id INT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BIT(1) NOT NULL DEFAULT b'0',
    revoked BIT(1) NOT NULL DEFAULT b'0',
    current_access_jti VARCHAR(36) NOT NULL,
    access_expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT refresh_token_users_FK FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX refresh_token_token_hash_IDX ON refresh_token (token_hash);
CREATE INDEX refresh_token_family_id_IDX ON refresh_token (family_id);
CREATE INDEX refresh_token_current_access_jti_IDX ON refresh_token (current_access_jti);
CREATE INDEX refresh_token_user_id_expires_at_IDX ON refresh_token (user_id, expires_at);
CREATE INDEX refresh_token_expires_at_IDX ON refresh_token (expires_at);
