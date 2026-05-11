-- ============================================================
-- MeghaConnect V29: Grievance audit columns
-- ============================================================
-- Grievance extends BaseEntity, so Hibernate selects created_by
-- and updated_by. Older databases created from V5 only have the
-- timestamp audit columns, so add the user audit columns safely.

DELIMITER $$

CREATE PROCEDURE add_grievance_audit_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'grievances'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE grievances ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_grievance_audit_column_if_missing('created_by', 'VARCHAR(100) NULL');
CALL add_grievance_audit_column_if_missing('updated_by', 'VARCHAR(100) NULL');

DROP PROCEDURE add_grievance_audit_column_if_missing;
