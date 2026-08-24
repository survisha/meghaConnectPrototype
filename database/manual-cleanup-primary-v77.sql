-- MANUAL DBA RUNBOOK ONLY. This file is never executed by application Flyway.
-- Use after confirming the legacy copy in meghaconnect_legacy is complete.

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name LIKE 'legacy\_%';

-- Compare counts between the primary and legacy databases before cleanup, e.g.:
-- SELECT (SELECT COUNT(*) FROM meghaconnect.legacy_import_batch) primary_count,
--        (SELECT COUNT(*) FROM meghaconnect_legacy.legacy_import_batch) legacy_count;

-- Export/copy existing primary legacy data using an approved DBA migration procedure.
-- Do not uncomment DROP statements until backups and count/hash reconciliation pass.
-- DROP TABLE meghaconnect.legacy_person_index;
-- DROP TABLE meghaconnect.legacy_dataset_record;
-- DROP TABLE meghaconnect.legacy_import_error;
-- DROP TABLE meghaconnect.legacy_import_column;
-- DROP TABLE meghaconnect.legacy_import_sheet;
-- DROP TABLE meghaconnect.legacy_import_batch;
-- DROP TABLE meghaconnect.legacy_dataset_column_alias;
-- DROP TABLE meghaconnect.legacy_dataset_column;
-- DROP TABLE meghaconnect.legacy_dataset_definition;
