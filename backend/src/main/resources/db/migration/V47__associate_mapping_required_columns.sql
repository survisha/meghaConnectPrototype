-- Make older associate_mappings tables compatible with the current AssociateMapping entity.
-- These columns are nullable so legacy rows remain readable.

DROP PROCEDURE IF EXISTS add_associate_mapping_required_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_associate_mapping_required_column_if_missing(
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

CALL add_associate_mapping_required_column_if_missing('person_id', 'BIGINT NULL');
CALL add_associate_mapping_required_column_if_missing('relationship', 'VARCHAR(200) NULL');
CALL add_associate_mapping_required_column_if_missing('associate_name', 'VARCHAR(200) NULL');
CALL add_associate_mapping_required_column_if_missing('associate_phone', 'VARCHAR(20) NULL');
CALL add_associate_mapping_required_column_if_missing('associate_epic', 'VARCHAR(50) NULL');
CALL add_associate_mapping_required_column_if_missing('associate_designation', 'VARCHAR(100) NULL');
CALL add_associate_mapping_required_column_if_missing('associate_address', 'VARCHAR(500) NULL');

DROP PROCEDURE IF EXISTS add_associate_mapping_required_column_if_missing;
