-- Requires MySQL 8.0.29+ for the `IF NOT EXISTS` clauses on ALTER TABLE ADD COLUMN/INDEX/CONSTRAINT.
-- Idempotent by design: this script is safe to run against a database that already has this
-- schema applied manually, as well as against a brand-new empty database.

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    account_non_expired BIT(1) NOT NULL DEFAULT b'0',
    account_non_locked BIT(1) NOT NULL DEFAULT b'0',
    credentials_non_expired BIT(1) NOT NULL DEFAULT b'1',
    enabled BIT(1) NOT NULL DEFAULT b'1'
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL;

ALTER TABLE users ADD UNIQUE INDEX IF NOT EXISTS users_username_IDX (username);

CREATE TABLE IF NOT EXISTS url_mapping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    long_url VARCHAR(2048) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    created_by INT,
    updated_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE url_mapping ADD UNIQUE INDEX IF NOT EXISTS url_mapping_short_code_IDX (short_code);
ALTER TABLE url_mapping ADD CONSTRAINT IF NOT EXISTS url_mapping_users_FK FOREIGN KEY (created_by) REFERENCES users(id);
