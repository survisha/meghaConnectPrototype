-- Align existing associate_mappings table with BaseEntity fields used by AssociateMapping.
-- Older databases may have this table without audit columns, causing Hibernate to query
-- created_by/updated_by/created_at/updated_at columns that do not exist.

DROP PROCEDURE IF EXISTS add_associate_mapping_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_associate_mapping_column_if_missing(
    IN column_name_to_add VARCHAR(64),
    IN column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'associate_mappings'
          AND column_name = column_name_to_add
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE associate_mappings ADD COLUMN ', column_name_to_add, ' ', column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_associate_mapping_column_if_missing('created_at', 'DATETIME NULL');
CALL add_associate_mapping_column_if_missing('updated_at', 'DATETIME NULL');
CALL add_associate_mapping_column_if_missing('created_by', 'VARCHAR(100) NULL');
CALL add_associate_mapping_column_if_missing('updated_by', 'VARCHAR(100) NULL');

UPDATE associate_mappings
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL OR updated_at IS NULL;

DROP PROCEDURE IF EXISTS add_associate_mapping_column_if_missing;
