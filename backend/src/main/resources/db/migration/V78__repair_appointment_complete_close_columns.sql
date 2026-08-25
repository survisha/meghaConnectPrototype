-- Repair environments where the appointment complete/close lifecycle entity
-- fields are present but the corresponding V77 schema changes are missing.
-- Every operation is conditional so partially migrated databases are safe.

DELIMITER //

CREATE PROCEDURE add_v78_appointment_column_if_missing(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'appointments'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE appointments ADD COLUMN ',
            p_column_name,
            ' ',
            p_column_definition
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

CREATE PROCEDURE add_v78_appointment_index_if_missing(
    IN p_index_name VARCHAR(64),
    IN p_index_columns TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'appointments'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT(
            'CREATE INDEX ',
            p_index_name,
            ' ON appointments (',
            p_index_columns,
            ')'
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

DELIMITER ;

CALL add_v78_appointment_column_if_missing('scheduled_by', 'VARCHAR(100) NULL');
CALL add_v78_appointment_column_if_missing('scheduled_at', 'TIMESTAMP NULL');
CALL add_v78_appointment_column_if_missing('rescheduled_by', 'VARCHAR(100) NULL');
CALL add_v78_appointment_column_if_missing('rescheduled_at', 'TIMESTAMP NULL');
CALL add_v78_appointment_column_if_missing('closed_by', 'VARCHAR(100) NULL');
CALL add_v78_appointment_column_if_missing('closed_at', 'TIMESTAMP NULL');
CALL add_v78_appointment_column_if_missing('final_remarks', 'TEXT NULL');

CALL add_v78_appointment_index_if_missing(
    'idx_appt_status_closed_at',
    'status, closed_at'
);

DROP PROCEDURE add_v78_appointment_index_if_missing;
DROP PROCEDURE add_v78_appointment_column_if_missing;
