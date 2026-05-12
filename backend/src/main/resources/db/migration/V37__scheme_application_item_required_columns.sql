-- ============================================================
-- MeghaConnect V37: Repair required scheme application item columns
-- ============================================================
-- Existing dev/test databases may have an older scheme_application_items
-- table that is missing columns mapped by SchemeApplicationItem.

DELIMITER $$

CREATE PROCEDURE add_scheme_item_required_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'scheme_application_items'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE scheme_application_items ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_scheme_item_required_column_if_missing('description', 'VARCHAR(300) NOT NULL DEFAULT ''Item''');
CALL add_scheme_item_required_column_if_missing('quantity', 'INT NOT NULL DEFAULT 1');
CALL add_scheme_item_required_column_if_missing('unit_cost', 'DECIMAL(14,2) NOT NULL DEFAULT 0.00');

DROP PROCEDURE add_scheme_item_required_column_if_missing;
