-- ============================================================
-- MeghaConnect V23: Visitor OTP resolved visitor identity
-- ============================================================
-- Login OTPs can now be tied to a specific visitor when one mobile
-- number is shared by multiple EPIC registrations. KYC/registration OTPs
-- continue to store NULL visitor_id.

DELIMITER $$

CREATE PROCEDURE add_otp_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visitor_otp_temp'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE visitor_otp_temp ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_otp_index_if_missing(
    IN p_index_name VARCHAR(100),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visitor_otp_temp'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX ', p_index_name, ' ON visitor_otp_temp ', p_index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_otp_column_if_missing('visitor_id', 'BIGINT NULL');
CALL add_otp_index_if_missing('idx_otp_phone_visitor', '(phone_number, visitor_id)');

DROP PROCEDURE add_otp_index_if_missing;
DROP PROCEDURE add_otp_column_if_missing;
