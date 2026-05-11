-- ============================================================
-- MeghaConnect V30: Link grievances to visitors
-- ============================================================
-- New citizen grievances should reference visitors.id instead of
-- duplicating citizen profile fields in the grievance row. Legacy
-- columns remain nullable for old/staff-created records.

DELIMITER $$

CREATE PROCEDURE add_grievance_column_if_missing(
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

CREATE PROCEDURE modify_grievance_column_if_exists(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'grievances'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE grievances MODIFY COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_grievance_index_if_missing(
    IN p_index_name VARCHAR(100),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'grievances'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE grievances ADD INDEX ', p_index_name, ' ', p_index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_grievance_fk_if_missing(
    IN p_constraint_name VARCHAR(100),
    IN p_constraint_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'grievances'
          AND CONSTRAINT_NAME = p_constraint_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE grievances ADD CONSTRAINT ', p_constraint_name, ' ', p_constraint_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_grievance_column_if_missing('visitor_id', 'BIGINT NULL');

CALL modify_grievance_column_if_exists('applicant_name', 'VARCHAR(200) NULL');
CALL modify_grievance_column_if_exists('category', 'VARCHAR(50) NULL');

UPDATE grievances g
JOIN (
    SELECT phone_number, MIN(id) AS visitor_id, COUNT(*) AS visitor_count
    FROM visitors
    WHERE phone_number IS NOT NULL
    GROUP BY phone_number
) v ON v.phone_number = g.phone_number AND v.visitor_count = 1
SET g.visitor_id = v.visitor_id
WHERE g.visitor_id IS NULL
  AND g.phone_number IS NOT NULL;

UPDATE grievances g
LEFT JOIN visitors v ON v.id = g.visitor_id
SET g.visitor_id = NULL
WHERE g.visitor_id IS NOT NULL
  AND v.id IS NULL;

CALL add_grievance_index_if_missing('idx_grievance_visitor', '(visitor_id)');
CALL add_grievance_fk_if_missing(
    'fk_grievance_visitor',
    'FOREIGN KEY (visitor_id) REFERENCES visitors(id) ON DELETE SET NULL'
);

DROP PROCEDURE add_grievance_column_if_missing;
DROP PROCEDURE modify_grievance_column_if_exists;
DROP PROCEDURE add_grievance_index_if_missing;
DROP PROCEDURE add_grievance_fk_if_missing;
