-- ============================================================
-- Department tenancy foundation
-- Non-destructive migration: creates tenant table, seeds default CMO
-- department, adds nullable department links, and backfills existing data.
-- ============================================================

CREATE TABLE IF NOT EXISTS departments (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    department_code   VARCHAR(50)  NOT NULL,
    department_name   VARCHAR(200) NOT NULL,
    description       TEXT,
    contact_email     VARCHAR(150),
    contact_mobile    VARCHAR(20),
    address           VARCHAR(500),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uq_departments_code (department_code),
    KEY idx_departments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO departments (
    department_code,
    department_name,
    description,
    status,
    created_at,
    created_by
)
VALUES (
    'CMO',
    'Chief Minister''s Office',
    'Default department for existing MeghaConnect CMO records.',
    'ACTIVE',
    NOW(),
    'flyway'
)
ON DUPLICATE KEY UPDATE
    department_name = VALUES(department_name),
    description = VALUES(description),
    status = 'ACTIVE',
    updated_at = NOW(),
    updated_by = 'flyway';

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('users', 'email', 'VARCHAR(150) NULL AFTER full_name');
CALL add_column_if_missing('users', 'department_id', 'BIGINT NULL AFTER role');
CALL add_column_if_missing('users', 'password_change_required', 'TINYINT(1) NOT NULL DEFAULT 0 AFTER locked');
CALL add_column_if_missing('appointments', 'department_id', 'BIGINT NULL AFTER applicant_id');
CALL add_column_if_missing('visitors', 'department_id', 'BIGINT NULL AFTER id');
CALL add_column_if_missing('schedule_events', 'department_id', 'BIGINT NULL AFTER id');
CALL add_column_if_missing('audit_logs', 'department_id', 'BIGINT NULL AFTER id');
CALL add_column_if_missing('appointment_qr_token', 'department_id', 'BIGINT NULL AFTER id');
CALL add_column_if_missing('qr_scan_audit_log', 'department_id', 'BIGINT NULL AFTER id');
CALL add_column_if_missing('visitor_movement_log', 'department_id', 'BIGINT NULL AFTER id');

DROP PROCEDURE IF EXISTS add_column_if_missing;

SET @cmo_department_id = (SELECT id FROM departments WHERE department_code = 'CMO');

UPDATE users
SET department_id = @cmo_department_id
WHERE department_id IS NULL
  AND role <> 'SUPER_ADMIN';

UPDATE appointments
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

UPDATE visitors
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

UPDATE schedule_events
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

UPDATE audit_logs
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

UPDATE appointment_qr_token
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

UPDATE qr_scan_audit_log
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

UPDATE visitor_movement_log
SET department_id = @cmo_department_id
WHERE department_id IS NULL;

INSERT INTO roles (role_name, description, created_at)
VALUES
    ('SUPER_ADMIN', 'System-level administrator across all departments', NOW()),
    ('DEPARTMENT_ADMIN', 'Department administrator for appointment management', NOW()),
    ('DEPARTMENT_PA', 'Department PA appointment workflow user', NOW())
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = NOW();

INSERT INTO users (
    username,
    password_hash,
    full_name,
    role,
    active,
    offline_access,
    locked,
    password_change_required,
    created_at,
    created_by
)
VALUES (
    'superaadmin',
    '$2a$10$b8dqFMAbhW6wj8ZKezLew.o8RVRfn2PcOACVjQb4UxsOOmoKSbyVS',
    'Super Admin',
    'SUPER_ADMIN',
    1,
    0,
    0,
    1,
    NOW(),
    'flyway'
)
ON DUPLICATE KEY UPDATE
    role = 'SUPER_ADMIN',
    active = 1,
    department_id = NULL,
    updated_at = NOW(),
    updated_by = 'flyway';

CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_appointments_department ON appointments(department_id);
CREATE INDEX idx_visitors_department ON visitors(department_id);
CREATE INDEX idx_schedule_events_department ON schedule_events(department_id);
CREATE INDEX idx_audit_logs_department ON audit_logs(department_id);
CREATE INDEX idx_appointment_qr_token_department ON appointment_qr_token(department_id);
CREATE INDEX idx_qr_scan_audit_department ON qr_scan_audit_log(department_id);
CREATE INDEX idx_visitor_movement_department ON visitor_movement_log(department_id);
