-- ============================================================
-- Walk-in registration, token sequence, and reference cleanup
-- ============================================================

INSERT INTO reference_data (type_id, code, value, display_order, is_active)
SELECT rt.id, 'INVITATION', 'Invitation', 0, TRUE
FROM reference_type rt
WHERE rt.code = 'CM_AGENDA_MEETING'
  AND NOT EXISTS (
      SELECT 1
      FROM reference_data rd
      WHERE rd.type_id = rt.id
        AND rd.code = 'INVITATION'
  );

CREATE TABLE IF NOT EXISTS walkin_token_sequence (
    token_date DATE NOT NULL,
    last_token_value INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (token_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS walkins (
    walkin_id BIGINT NOT NULL AUTO_INCREMENT,
    visitor_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,
    token_number VARCHAR(40) NOT NULL,
    token_date DATE NOT NULL,
    name VARCHAR(200) NOT NULL,
    mobile VARCHAR(20),
    id_type VARCHAR(20),
    agenda_type VARCHAR(200),
    brief_description TEXT,
    created_by_deo_id VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (walkin_id),
    UNIQUE KEY uq_walkin_token_date (token_date, token_number),
    UNIQUE KEY uq_walkin_appointment (appointment_id),
    KEY idx_walkin_visitor (visitor_id),
    KEY idx_walkin_created_at (created_at),
    CONSTRAINT fk_walkin_visitor FOREIGN KEY (visitor_id) REFERENCES visitors(id),
    CONSTRAINT fk_walkin_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER //

CREATE PROCEDURE add_v50_visitor_column_if_missing(
    IN p_column_name VARCHAR(64),
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
END//

CREATE PROCEDURE add_v50_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX ', p_index_name, ' ON ', p_table_name, ' ', p_index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE drop_v50_walkin_column_if_exists(IN p_column_name VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'walkins'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE walkins DROP COLUMN ', p_column_name);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL drop_v50_walkin_column_if_exists('kyc_status');

CALL add_v50_visitor_column_if_missing('agenda_type', 'VARCHAR(200) NULL');
CALL add_v50_visitor_column_if_missing('brief_description', 'TEXT NULL');

CALL add_v50_index_if_missing('visitors', 'idx_visitor_created_at', '(created_at)');
CALL add_v50_index_if_missing('appointments', 'idx_appointment_created_at', '(created_at)');
CALL add_v50_index_if_missing('appointments', 'idx_appointment_scheduled_date', '(scheduled_date_time)');

DROP PROCEDURE IF EXISTS add_v50_visitor_column_if_missing;
DROP PROCEDURE IF EXISTS add_v50_index_if_missing;
DROP PROCEDURE IF EXISTS drop_v50_walkin_column_if_exists;
