-- ============================================================
-- MeghaConnect V27: Appointment flow audit columns
-- ============================================================
-- Some UAT databases were created before BaseEntity audit columns
-- were added consistently. Keep these idempotent so inserts from
-- Appointment, SchemeApplication, and DocumentUpload can persist.

DELIMITER $$

CREATE PROCEDURE add_audit_column_if_missing(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
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

CALL add_audit_column_if_missing('appointments', 'created_by', 'VARCHAR(100) NULL');
CALL add_audit_column_if_missing('appointments', 'updated_by', 'VARCHAR(100) NULL');

CALL add_audit_column_if_missing('scheme_applications', 'created_by', 'VARCHAR(100) NULL');
CALL add_audit_column_if_missing('scheme_applications', 'updated_by', 'VARCHAR(100) NULL');

CALL add_audit_column_if_missing('document_uploads', 'created_by', 'VARCHAR(100) NULL');
CALL add_audit_column_if_missing('document_uploads', 'updated_by', 'VARCHAR(100) NULL');

DROP PROCEDURE add_audit_column_if_missing;
