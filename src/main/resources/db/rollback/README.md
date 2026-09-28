# Rollback scripts

This folder holds manual rollback scripts paired with the forward migrations in
`src/main/resources/db/migration`. Flyway is only configured to scan `db/migration`
(`spring.flyway.locations: classpath:db/migration`) — it never reads or executes anything
in this folder.

Flyway Community edition does not support automatic "undo" migrations (that's a Flyway
Teams/Enterprise feature), so rollbacks here are applied by hand when needed, e.g.:

```bash
mysql -u <user> -p url_shortner < V1__initial_schema_rollback.sql
```

## Convention

Every forward migration `V{n}__description.sql` added to `db/migration` must get a matching
`V{n}__description_rollback.sql` here containing the inverse DDL, applied in reverse order of
the forward script's statements.

## MySQL `IF [NOT] EXISTS` on ALTER TABLE

Standard MySQL (unlike MariaDB) does not support `IF NOT EXISTS`/`IF EXISTS` on
`ALTER TABLE ADD/DROP COLUMN/INDEX/CONSTRAINT` — it's a syntax error. `CREATE TABLE IF NOT EXISTS`
and `DROP TABLE IF EXISTS` are fine and don't need this. For a guarded `ALTER TABLE`, wrap it in a
throwaway stored procedure that checks `INFORMATION_SCHEMA` first, following the pattern already
used in `V1__initial_schema.sql`/`V1__initial_schema_rollback.sql`:

```sql
DELIMITER $$
CREATE PROCEDURE v{n}_apply_idempotent_changes()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'some_table' AND COLUMN_NAME = 'some_column'
    ) THEN
        ALTER TABLE some_table ADD COLUMN some_column INT;
    END IF;
END$$
DELIMITER ;

CALL v{n}_apply_idempotent_changes();
DROP PROCEDURE v{n}_apply_idempotent_changes;
```
