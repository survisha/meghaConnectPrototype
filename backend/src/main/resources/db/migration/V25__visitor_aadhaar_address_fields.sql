-- ============================================================
-- MeghaConnect V25: Visitor Aadhaar address fields
-- ============================================================
-- Stores Aadhaar KYC address components as file-safe metadata.
-- Existing address/address_line columns remain for backward compatibility.

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

CALL add_visitor_column_if_missing('full_address', 'VARCHAR(500)');
CALL add_visitor_column_if_missing('address1', 'VARCHAR(500)');
CALL add_visitor_column_if_missing('city', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('pincode', 'VARCHAR(10)');

DROP PROCEDURE add_visitor_column_if_missing;
