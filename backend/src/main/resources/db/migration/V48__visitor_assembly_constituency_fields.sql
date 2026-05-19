-- ============================================================
-- MeghaConnect V48: Visitor assembly constituency metadata
-- ============================================================
-- Stores assembly constituency values returned by EPIC KYC.

DELIMITER $$

CREATE PROCEDURE add_visitor_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visitors'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE visitors ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_visitor_column_if_missing('assembly_constituency_number', 'VARCHAR(50)');
CALL add_visitor_column_if_missing('assembly_constituency_name', 'VARCHAR(200)');

DROP PROCEDURE add_visitor_column_if_missing;
