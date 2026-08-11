-- Canonical lifecycle foundation. Existing appointment types and Public Darbar
-- records are intentionally preserved for a later business decision.

DELIMITER //

CREATE PROCEDURE add_v72_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

DELIMITER ;

CALL add_v72_column_if_missing(
    'appointments',
    'appointment_category',
    "VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' AFTER appointment_type"
);

-- Categorize without changing appointment-type reference data.
UPDATE appointments
SET appointment_category = CASE
    WHEN public_darbar_id IS NOT NULL THEN 'PUBLIC_DARBAR'
    WHEN COALESCE(is_walk_in, 0) = 1 THEN 'WALK_IN'
    ELSE 'SCHEDULED'
END;

-- Public Darbar retains its existing states. All other active appointments are
-- normalized into the approved frozen lifecycle. Original values remain in
-- appointment_audit/audit_logs for historical traceability.
UPDATE appointments
SET status = CASE
    WHEN appointment_category = 'WALK_IN' AND status = 'COMPLETED' THEN 'COMPLETED'
    WHEN appointment_category = 'WALK_IN' THEN 'PENDING'
    WHEN appointment_category = 'SCHEDULED' AND status = 'COMPLETED' THEN 'HCM_MET_COMPLETED'
    WHEN appointment_category = 'SCHEDULED' AND status = 'FORWARDED_TO_DEPARTMENT' THEN 'ROUTED_TO_OFFICIAL'
    WHEN appointment_category = 'SCHEDULED' AND status IN ('REJECTED', 'HCM_REJECTED', 'CANCELLED') THEN 'REJECTED'
    WHEN appointment_category = 'SCHEDULED'
         AND status IN ('APPROVED_WITH_DATE_TIME', 'SCHEDULED', 'HCM_ACCEPTED') THEN 'SCHEDULED'
    WHEN appointment_category = 'SCHEDULED' THEN 'PENDING'
    ELSE status
END;

UPDATE walkins
SET status = CASE WHEN status = 'COMPLETED' THEN 'COMPLETED' ELSE 'PENDING' END;

CREATE INDEX idx_appt_category_status_created
    ON appointments (appointment_category, status, created_at);

CREATE INDEX idx_walkin_status_created
    ON walkins (status, created_at);

DROP PROCEDURE add_v72_column_if_missing;
