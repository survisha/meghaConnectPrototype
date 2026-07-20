-- Standardize the Super Admin bootstrap account after the legacy "superaadmin" typo.

INSERT INTO roles (role_name, description, created_at)
VALUES ('SUPER_ADMIN', 'System-level administrator across all departments', NOW())
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = NOW();

UPDATE audit_logs
SET performed_by = 'superadmin'
WHERE LOWER(TRIM(performed_by)) = 'superaadmin';

UPDATE appointment_audit
SET performed_by = 'superadmin'
WHERE LOWER(TRIM(performed_by)) = 'superaadmin';

UPDATE users
SET username = 'superadmin',
    role = 'SUPER_ADMIN',
    active = 1,
    locked = 0,
    department_id = NULL,
    updated_at = NOW(),
    updated_by = 'flyway'
WHERE LOWER(TRIM(username)) = 'superaadmin'
  AND NOT EXISTS (
      SELECT 1
      FROM (SELECT id FROM users WHERE LOWER(TRIM(username)) = 'superadmin') canonical
  );

UPDATE users
SET role = 'SUPER_ADMIN',
    active = 1,
    locked = 0,
    department_id = NULL,
    updated_at = NOW(),
    updated_by = 'flyway'
WHERE LOWER(TRIM(username)) = 'superadmin';

UPDATE users
SET username = CONCAT('superaadmin-inactive-', id),
    active = 0,
    locked = 1,
    department_id = NULL,
    updated_at = NOW(),
    updated_by = 'flyway'
WHERE LOWER(TRIM(username)) = 'superaadmin'
  AND EXISTS (
      SELECT 1
      FROM (SELECT id FROM users WHERE LOWER(TRIM(username)) = 'superadmin') canonical
  );
