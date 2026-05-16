-- ============================================================
-- MeghaConnect V42: HCM/OSD action notes and department forwarding
-- ============================================================

DELIMITER $$

CREATE PROCEDURE add_hcm_action_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'hcm_actions'
    ) AND NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'hcm_actions'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE hcm_actions ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_hcm_action_column_if_missing('decision', 'VARCHAR(200) NULL');
CALL add_hcm_action_column_if_missing('department_code', 'VARCHAR(100) NULL');
CALL add_hcm_action_column_if_missing('department_name', 'VARCHAR(200) NULL');
CALL add_hcm_action_column_if_missing('created_by', 'VARCHAR(100) NULL');
CALL add_hcm_action_column_if_missing('created_by_role', 'VARCHAR(50) NULL');

DROP PROCEDURE add_hcm_action_column_if_missing;
