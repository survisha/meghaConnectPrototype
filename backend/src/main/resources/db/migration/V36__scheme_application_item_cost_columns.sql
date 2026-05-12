-- ============================================================
-- MeghaConnect V36: Repair scheme application item cost columns
-- ============================================================
-- Some existing environments have scheme_application_items from an
-- older schema without moderation/approval cost columns. Hibernate maps
-- these fields, so list APIs fail until the columns exist.

DELIMITER $$

CREATE PROCEDURE add_scheme_item_column_if_missing(
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

CALL add_scheme_item_column_if_missing('cmo_moderated_unit_cost', 'DECIMAL(14,2) NULL');
CALL add_scheme_item_column_if_missing('hcm_approved_unit_cost', 'DECIMAL(14,2) NULL');

DROP PROCEDURE add_scheme_item_column_if_missing;
