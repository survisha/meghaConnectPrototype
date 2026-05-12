-- ============================================================
-- MeghaConnect V33: Secure document file metadata
-- ============================================================
-- New uploads store normal binary files on disk. The database stores only
-- metadata plus an encrypted relative file key and HMAC for tamper detection.

DELIMITER $$

CREATE PROCEDURE add_document_secure_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'document_uploads'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE document_uploads ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE modify_document_secure_column_if_exists(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'document_uploads'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE document_uploads MODIFY COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_document_secure_column_if_missing('stored_file_name', 'VARCHAR(300) NULL AFTER original_filename');
CALL add_document_secure_column_if_missing('encrypted_file_path', 'VARCHAR(1000) NULL AFTER file_path');
CALL add_document_secure_column_if_missing('secure_hash', 'VARCHAR(128) NULL AFTER encrypted_file_path');
CALL add_document_secure_column_if_missing('content_type', 'VARCHAR(100) NULL AFTER mime_type');
CALL add_document_secure_column_if_missing('uploaded_date', 'DATETIME NULL AFTER uploaded_by');

CALL modify_document_secure_column_if_exists('file_path', 'VARCHAR(1000) NOT NULL');
CALL modify_document_secure_column_if_exists('original_filename', 'VARCHAR(300) NULL');

UPDATE document_uploads
SET content_type = mime_type
WHERE content_type IS NULL
  AND mime_type IS NOT NULL;

UPDATE document_uploads
SET uploaded_date = created_at
WHERE uploaded_date IS NULL;

UPDATE document_uploads
SET stored_file_name = SUBSTRING_INDEX(file_path, '/', -1)
WHERE stored_file_name IS NULL
  AND encrypted_file_path IS NULL
  AND file_path IS NOT NULL
  AND file_path NOT LIKE 'enc:%';

DROP PROCEDURE add_document_secure_column_if_missing;
DROP PROCEDURE modify_document_secure_column_if_exists;
