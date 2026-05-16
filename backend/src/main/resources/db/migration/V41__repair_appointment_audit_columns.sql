-- ============================================================
-- MeghaConnect V41: Repair appointment audit columns
-- ============================================================
-- Some environments have Flyway history from before the appointment
-- audit repair migrations were complete. Keep this migration idempotent
-- so Hibernate BaseEntity mappings always have their expected columns.

DELIMITER $$

CREATE PROCEDURE add_appointment_audit_column_if_missing(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
    ) AND NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_appointment_audit_column_if_missing('appointments', 'created_by', 'VARCHAR(100) NULL');
CALL add_appointment_audit_column_if_missing('appointments', 'updated_by', 'VARCHAR(100) NULL');

CALL add_appointment_audit_column_if_missing('scheme_applications', 'created_by', 'VARCHAR(100) NULL');
CALL add_appointment_audit_column_if_missing('scheme_applications', 'updated_by', 'VARCHAR(100) NULL');

CALL add_appointment_audit_column_if_missing('document_uploads', 'created_by', 'VARCHAR(100) NULL');
CALL add_appointment_audit_column_if_missing('document_uploads', 'updated_by', 'VARCHAR(100) NULL');

CALL add_appointment_audit_column_if_missing('appointment_document_ai_notes', 'created_by', 'VARCHAR(100) NULL');
CALL add_appointment_audit_column_if_missing('appointment_document_ai_notes', 'updated_by', 'VARCHAR(100) NULL');

DROP PROCEDURE add_appointment_audit_column_if_missing;
