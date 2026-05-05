-- ============================================================
-- MeghaConnect V22: Visitor registration photo/address fields
-- ============================================================
-- Adds file-path and location metadata for the EPIC/Aadhaar visitor
-- registration flow. Existing columns and data are preserved.

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

CALL add_visitor_column_if_missing('live_photo_path', 'VARCHAR(500)');
CALL add_visitor_column_if_missing('address_line', 'VARCHAR(500)');
CALL add_visitor_column_if_missing('booth_village', 'VARCHAR(200)');
CALL add_visitor_column_if_missing('outside_meghalaya', 'TINYINT(1) DEFAULT 0');
CALL add_visitor_column_if_missing('location', 'VARCHAR(255)');

DROP PROCEDURE add_visitor_column_if_missing;
