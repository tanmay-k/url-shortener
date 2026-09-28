-- Manual rollback for V1__initial_schema.sql.
-- Flyway never runs this file (only src/main/resources/db/migration is on its scan path).
-- Apply by hand if you need to undo V1, e.g.:
--   mysql -u <user> -p url_shortner < V1__initial_schema_rollback.sql
--
-- NOTE: standard MySQL (unlike MariaDB) does not support `IF EXISTS` on
-- ALTER TABLE DROP FOREIGN KEY/INDEX, so those guards are implemented via
-- INFORMATION_SCHEMA checks inside a throwaway stored procedure instead.

DELIMITER $$

CREATE PROCEDURE v1_rollback_idempotent_changes()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'url_mapping' AND CONSTRAINT_NAME = 'url_mapping_users_FK'
    ) THEN
        ALTER TABLE url_mapping DROP FOREIGN KEY url_mapping_users_FK;
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'url_mapping' AND INDEX_NAME = 'url_mapping_short_code_IDX'
    ) THEN
        ALTER TABLE url_mapping DROP INDEX url_mapping_short_code_IDX;
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'users_username_IDX'
    ) THEN
        ALTER TABLE users DROP INDEX users_username_IDX;
    END IF;
END$$

DELIMITER ;

CALL v1_rollback_idempotent_changes();
DROP PROCEDURE v1_rollback_idempotent_changes;

DROP TABLE IF EXISTS url_mapping;
DROP TABLE IF EXISTS users;
