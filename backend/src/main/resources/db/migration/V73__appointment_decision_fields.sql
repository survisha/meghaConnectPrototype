-- Decision data supporting the frozen appointment lifecycle.
DELIMITER //
CREATE PROCEDURE add_v73_column_if_missing(IN p_name VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments' AND COLUMN_NAME = p_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE appointments ADD COLUMN ', p_name, ' ', p_definition);
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//
DELIMITER ;

CALL add_v73_column_if_missing('routed_department_id', 'BIGINT NULL');
CALL add_v73_column_if_missing('routed_officer', 'VARCHAR(200) NULL');
CALL add_v73_column_if_missing('return_reason', 'TEXT NULL');
CALL add_v73_column_if_missing('required_information', 'TEXT NULL');
CALL add_v73_column_if_missing('return_due_date', 'DATE NULL');
CALL add_v73_column_if_missing('meeting_outcome', 'TEXT NULL');
CALL add_v73_column_if_missing('completed_at', 'DATETIME NULL');
CALL add_v73_column_if_missing('completed_by', 'VARCHAR(100) NULL');
CALL add_v73_column_if_missing('follow_up_required', 'BOOLEAN NOT NULL DEFAULT FALSE');

DROP PROCEDURE add_v73_column_if_missing;

CREATE INDEX idx_appt_routed_department_status
    ON appointments (routed_department_id, status);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appt_routed_department
    FOREIGN KEY (routed_department_id) REFERENCES departments(id);
