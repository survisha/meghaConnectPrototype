-- ============================================================
-- MeghaConnect V21: Visitor KYC metadata fields
-- ============================================================
-- Stores only the final registration data after KYC completion.
-- Aadhaar identity is stored masked by the application service.

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

CALL add_visitor_column_if_missing('gender', 'VARCHAR(20)');
CALL add_visitor_column_if_missing('state', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('borrower_address_house_number', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('borrower_address_section_number', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('relative_name_on_voter_id', 'VARCHAR(200)');
CALL add_visitor_column_if_missing('polling_part_no', 'VARCHAR(50)');
CALL add_visitor_column_if_missing('polling_station_address', 'VARCHAR(500)');
CALL add_visitor_column_if_missing('voter_id_verification_request_id', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('voter_id_verification_completion_timestamp', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('name_match_score', 'INT');
CALL add_visitor_column_if_missing('id_found', 'TINYINT(1)');
CALL add_visitor_column_if_missing('aadhaar_client_txn_id', 'VARCHAR(100)');
CALL add_visitor_column_if_missing('aadhaar_app_id', 'VARCHAR(50)');
CALL add_visitor_column_if_missing('masked_identity_number', 'VARCHAR(50)');

DROP PROCEDURE add_visitor_column_if_missing;
