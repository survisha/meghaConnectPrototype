-- ============================================================
-- MeghaConnect V31: Drop redundant grievance profile columns
-- ============================================================
-- Citizen profile data now lives on visitors and is reached through
-- grievances.visitor_id. Keep the grievance table focused on the
-- grievance itself.

DELIMITER $$

CREATE PROCEDURE drop_grievance_index_if_exists(
    IN p_index_name VARCHAR(100)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'grievances'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE grievances DROP INDEX ', p_index_name);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE drop_grievance_column_if_exists(
    IN p_column_name VARCHAR(100)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'grievances'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE grievances DROP COLUMN `', p_column_name, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL drop_grievance_index_if_exists('idx_grievance_phone');

CALL drop_grievance_column_if_exists('applicant_name');
CALL drop_grievance_column_if_exists('phone_number');
CALL drop_grievance_column_if_exists('district');
CALL drop_grievance_column_if_exists('constituency');
CALL drop_grievance_column_if_exists('category');

DROP PROCEDURE drop_grievance_index_if_exists;
DROP PROCEDURE drop_grievance_column_if_exists;
