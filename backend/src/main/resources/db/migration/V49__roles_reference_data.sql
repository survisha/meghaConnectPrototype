-- ============================================================
-- Roles reference table for dynamic user management
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    role_id      BIGINT       NOT NULL AUTO_INCREMENT,
    role_name    VARCHAR(50)  NOT NULL UNIQUE,
    description  TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO roles (role_name, description, created_at)
VALUES
    ('HCM', 'Hon. Chief Minister', NOW()),
    ('ADMIN', 'System Administrator', NOW()),
    ('OSD', 'Officer on Special Duty', NOW()),
    ('APPROVER', 'Approval workflow user', NOW()),
    ('CMO_OFFICER', 'CMO Officer', NOW()),
    ('CMO', 'Chief Minister Office user', NOW()),
    ('DATA_ENTRY_OPERATOR', 'Data Entry Operator', NOW()),
    ('SECURITY', 'Security scanner user', NOW()),
    ('PUBLIC', 'Public visitor user', NOW()),
    ('CITIZEN', 'Citizen user', NOW())
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = NOW();

DELETE FROM roles WHERE role_name IN ('SAIDUL_OSD', 'APPROVER_JT_SECY');
