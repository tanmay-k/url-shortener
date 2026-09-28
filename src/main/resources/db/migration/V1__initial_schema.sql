-- Idempotent by design: this script is safe to run against a database that already has this
-- schema applied manually, as well as against a brand-new empty database.
--
-- NOTE: standard MySQL (unlike MariaDB) does not support `IF NOT EXISTS` on
-- ALTER TABLE ADD COLUMN/INDEX/CONSTRAINT, so those guards are implemented via
-- INFORMATION_SCHEMA checks inside a throwaway stored procedure instead.

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    account_non_expired BIT(1) NOT NULL DEFAULT b'0',
    account_non_locked BIT(1) NOT NULL DEFAULT b'0',
    credentials_non_expired BIT(1) NOT NULL DEFAULT b'1',
    enabled BIT(1) NOT NULL DEFAULT b'1'
);

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

DELIMITER $$

CREATE PROCEDURE v1_apply_idempotent_changes()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'created_at'
    ) THEN
        ALTER TABLE users ADD COLUMN created_at DATETIME NOT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'updated_at'
    ) THEN
        ALTER TABLE users ADD COLUMN updated_at DATETIME NOT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'users_username_IDX'
    ) THEN
        ALTER TABLE users ADD UNIQUE INDEX users_username_IDX (username);
    END IF;

    -- ALTER TABLE url_mapping ADD UNIQUE INDEX url_mapping_unique_IDX (long_url);

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'url_mapping' AND INDEX_NAME = 'url_mapping_short_code_IDX'
    ) THEN
        ALTER TABLE url_mapping ADD UNIQUE INDEX url_mapping_short_code_IDX (short_code);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'url_mapping' AND CONSTRAINT_NAME = 'url_mapping_users_FK'
    ) THEN
        ALTER TABLE url_mapping ADD CONSTRAINT url_mapping_users_FK FOREIGN KEY (created_by) REFERENCES users(id);
    END IF;
END$$

DELIMITER ;

CALL v1_apply_idempotent_changes();
DROP PROCEDURE v1_apply_idempotent_changes;
