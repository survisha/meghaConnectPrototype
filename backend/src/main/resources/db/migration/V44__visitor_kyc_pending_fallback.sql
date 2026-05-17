-- ============================================================
-- MeghaConnect V44: Visitor KYC pending fallback metadata
-- ============================================================

DELIMITER $$

CREATE PROCEDURE add_visitor_kyc_fallback_column_if_missing(
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

CALL add_visitor_kyc_fallback_column_if_missing('kyc_provider', 'VARCHAR(20) NULL');
CALL add_visitor_kyc_fallback_column_if_missing('kyc_failure_reason', 'TEXT NULL');
CALL add_visitor_kyc_fallback_column_if_missing('kyc_request_id', 'VARCHAR(100) NULL');
CALL add_visitor_kyc_fallback_column_if_missing('kyc_last_attempt_at', 'DATETIME NULL');

UPDATE visitors
SET kyc_provider = kyc_type
WHERE kyc_provider IS NULL
  AND kyc_type IS NOT NULL
  AND kyc_type <> 'NONE';

DROP PROCEDURE add_visitor_kyc_fallback_column_if_missing;
