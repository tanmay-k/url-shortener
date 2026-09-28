-- Manual rollback for V1__initial_schema.sql.
-- Flyway never runs this file (only src/main/resources/db/migration is on its scan path).
-- Apply by hand if you need to undo V1, e.g.:
--   mysql -u <user> -p url_shortner < V1__initial_schema_rollback.sql

ALTER TABLE url_mapping DROP FOREIGN KEY IF EXISTS url_mapping_users_FK;
ALTER TABLE url_mapping DROP INDEX IF EXISTS url_mapping_short_code_IDX;
DROP TABLE IF EXISTS url_mapping;

ALTER TABLE users DROP INDEX IF EXISTS users_username_IDX;
DROP TABLE IF EXISTS users;
