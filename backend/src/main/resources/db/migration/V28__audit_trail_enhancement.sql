-- ============================================================
-- MeghaConnect V28: Audit trail enhancement
-- ============================================================
-- Keeps the existing /api/v1/audit-logs API stable while adding
-- UI-facing audit metadata for filtering and detail views.

DELIMITER $$

CREATE PROCEDURE add_audit_log_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'audit_logs'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE audit_logs ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE modify_audit_log_column_if_exists(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'audit_logs'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE audit_logs MODIFY COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE copy_audit_log_column_if_possible(
    IN p_target_column VARCHAR(100),
    IN p_source_column VARCHAR(100)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'audit_logs'
          AND COLUMN_NAME = p_target_column
    ) AND EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'audit_logs'
          AND COLUMN_NAME = p_source_column
    ) THEN
        SET @ddl = CONCAT(
            'UPDATE audit_logs SET `', p_target_column, '` = `', p_source_column,
            '` WHERE `', p_target_column, '` IS NULL AND `', p_source_column, '` IS NOT NULL'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_audit_log_index_if_missing(
    IN p_index_name VARCHAR(100),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'audit_logs'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE audit_logs ADD INDEX ', p_index_name, ' ', p_index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_audit_log_column_if_missing('action_type', 'VARCHAR(100) NULL');
CALL add_audit_log_column_if_missing('description', 'TEXT NULL');
CALL add_audit_log_column_if_missing('username', 'VARCHAR(100) NULL');
CALL add_audit_log_column_if_missing('created_at', 'DATETIME NULL');
CALL add_audit_log_column_if_missing('user_role', 'VARCHAR(50) NULL');
CALL add_audit_log_column_if_missing('request_id', 'VARCHAR(128) NULL');
CALL add_audit_log_column_if_missing('old_value', 'TEXT NULL');
CALL add_audit_log_column_if_missing('new_value', 'TEXT NULL');
CALL add_audit_log_column_if_missing('status', 'VARCHAR(30) NULL DEFAULT ''SUCCESS''');
CALL add_audit_log_column_if_missing('endpoint', 'VARCHAR(300) NULL');
CALL add_audit_log_column_if_missing('ip_address', 'VARCHAR(50) NULL');

CALL copy_audit_log_column_if_possible('action_type', 'action');
CALL copy_audit_log_column_if_possible('description', 'details');
CALL copy_audit_log_column_if_possible('username', 'performed_by');
CALL copy_audit_log_column_if_possible('created_at', 'timestamp');

UPDATE audit_logs SET action_type = 'UNKNOWN' WHERE action_type IS NULL;
UPDATE audit_logs SET username = 'system' WHERE username IS NULL;
UPDATE audit_logs SET created_at = NOW() WHERE created_at IS NULL;
UPDATE audit_logs SET status = 'SUCCESS' WHERE status IS NULL;

CALL modify_audit_log_column_if_exists('action', 'VARCHAR(100) NULL');
CALL modify_audit_log_column_if_exists('performed_by', 'VARCHAR(100) NULL');
CALL modify_audit_log_column_if_exists('timestamp', 'DATETIME NULL');
CALL modify_audit_log_column_if_exists('action_type', 'VARCHAR(100) NOT NULL');
CALL modify_audit_log_column_if_exists('username', 'VARCHAR(100) NOT NULL');
CALL modify_audit_log_column_if_exists('created_at', 'DATETIME NOT NULL');

CALL add_audit_log_index_if_missing('idx_audit_created_at', '(created_at)');
CALL add_audit_log_index_if_missing('idx_audit_request', '(request_id)');
CALL add_audit_log_index_if_missing('idx_audit_action_type', '(action_type)');
CALL add_audit_log_index_if_missing('idx_audit_user_role', '(user_role)');

DROP PROCEDURE add_audit_log_column_if_missing;
DROP PROCEDURE modify_audit_log_column_if_exists;
DROP PROCEDURE copy_audit_log_column_if_possible;
DROP PROCEDURE add_audit_log_index_if_missing;
