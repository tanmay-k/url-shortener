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
