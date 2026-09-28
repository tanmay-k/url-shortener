CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    account_non_expired BIT(1) NOT NULL DEFAULT b'0',
    account_non_locked BIT(1) NOT NULL DEFAULT b'0',
    credentials_non_expired BIT(1) NOT NULL DEFAULT b'1',
    enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE UNIQUE INDEX users_username_IDX USING BTREE ON users (username);

CREATE TABLE url_mapping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    long_url VARCHAR(2048) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    created_by INT,
    updated_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- CREATE UNIQUE INDEX url_mapping_unique_IDX USING BTREE ON url_mapping (long_url);
CREATE UNIQUE INDEX url_mapping_short_code_IDX USING BTREE ON url_mapping (short_code);
ALTER TABLE url_mapping ADD CONSTRAINT url_mapping_users_FK FOREIGN KEY (created_by) REFERENCES users(id);
