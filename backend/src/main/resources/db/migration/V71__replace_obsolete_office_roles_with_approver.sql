-- APPROVER is the canonical replacement for the obsolete OSD, CMO and
-- CMO_OFFICER user roles. Preserve accounts and historical audit records.
INSERT INTO roles (role_name, description, created_at)
VALUES ('APPROVER', 'Approval workflow user', NOW())
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = NOW();

UPDATE users
SET role = 'APPROVER',
    updated_at = NOW(),
    updated_by = 'flyway-v71'
WHERE role IN ('OSD', 'CMO', 'CMO_OFFICER');

DELETE FROM roles
WHERE role_name IN ('OSD', 'CMO', 'CMO_OFFICER');
