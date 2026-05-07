-- ============================================================
-- MeghaConnect V26: Appointment document upload fields
-- ============================================================
-- Aligns document_uploads with the DocumentUpload entity used by
-- appointment submissions. Existing legacy entity_type/entity_id
-- columns are retained for backward compatibility.

DELIMITER $$

CREATE PROCEDURE add_document_upload_column_if_missing(
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

CREATE PROCEDURE modify_document_upload_column_if_exists(
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

CREATE PROCEDURE add_document_upload_index_if_missing(
    IN p_index_name VARCHAR(100),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'document_uploads'
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE document_uploads ADD INDEX ', p_index_name, ' ', p_index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_document_upload_column_if_missing('visitor_id', 'BIGINT NULL');
CALL add_document_upload_column_if_missing('appointment_id', 'BIGINT NULL');
CALL add_document_upload_column_if_missing('scheme_app_id', 'BIGINT NULL');
CALL add_document_upload_column_if_missing('file_size_bytes', 'BIGINT NULL');

CALL modify_document_upload_column_if_exists('entity_type', 'VARCHAR(50) NULL COMMENT ''Legacy entity type: APPOINTMENT, SCHEME_APPLICATION, PERSON''');
CALL modify_document_upload_column_if_exists('entity_id', 'BIGINT NULL');
CALL modify_document_upload_column_if_exists('original_filename', 'VARCHAR(300) NULL');
CALL modify_document_upload_column_if_exists('uploaded_by', 'VARCHAR(100) NULL');
CALL modify_document_upload_column_if_exists('updated_at', 'DATETIME NULL');

CALL add_document_upload_index_if_missing('idx_appointment_id_doc', '(appointment_id)');
CALL add_document_upload_index_if_missing('idx_visitor_id_doc', '(visitor_id)');

DROP PROCEDURE add_document_upload_column_if_missing;
DROP PROCEDURE modify_document_upload_column_if_exists;
DROP PROCEDURE add_document_upload_index_if_missing;
