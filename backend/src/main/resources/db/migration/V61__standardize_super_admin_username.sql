-- Standardize the Super Admin bootstrap account after the legacy "superaadmin" typo.
-- Schema notes:
--   * users.role is an enum string, not a role_id foreign key.
--   * AuditLog.performedBy maps to audit_logs.username since V28.
--   * appointment_audit retains its physical performed_by column.

INSERT INTO roles (role_name, description, created_at)
VALUES ('SUPER_ADMIN', 'System-level administrator across all departments', NOW())
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = NOW();

UPDATE audit_logs
SET username = 'superadmin'
WHERE LOWER(TRIM(username)) = 'superaadmin';

UPDATE appointment_audit
SET performed_by = 'superadmin'
WHERE LOWER(TRIM(performed_by)) = 'superaadmin';

-- Promote the legacy account only when no canonical account is present.
UPDATE users
SET username = 'superadmin',
    password_hash = '$2a$10$b9aCgvYJRYOgpWiDQQKqOuTkYy/.vvXHSog9hC5AnzHYOi2HS/Jue',
    full_name = 'Super Admin',
    role = 'SUPER_ADMIN',
    active = 1,
    locked = 0,
    password_change_required = 1,
    department_id = NULL,
    updated_at = NOW(),
    updated_by = 'flyway'
WHERE LOWER(TRIM(username)) = 'superaadmin'
  AND NOT EXISTS (
      SELECT 1
      FROM (SELECT id FROM users WHERE LOWER(TRIM(username)) = 'superadmin') canonical
  );

-- Create the account only when neither the canonical nor migrated account exists.
INSERT INTO users (
    username,
    password_hash,
    full_name,
    role,
    department_id,
    active,
    offline_access,
    locked,
    password_change_required,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    'superadmin',
    '$2a$10$b9aCgvYJRYOgpWiDQQKqOuTkYy/.vvXHSog9hC5AnzHYOi2HS/Jue',
    'Super Admin',
    'SUPER_ADMIN',
    NULL,
    1,
    0,
    0,
    1,
    NOW(),
    NOW(),
    'flyway',
    'flyway'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE LOWER(TRIM(username)) = 'superadmin'
);

-- Upgrade only a Flyway-created temporary account (including the exact V60
-- seed). User-managed canonical accounts retain their existing password.
UPDATE users
SET password_hash = '$2a$10$b9aCgvYJRYOgpWiDQQKqOuTkYy/.vvXHSog9hC5AnzHYOi2HS/Jue',
    password_change_required = 1,
    department_id = NULL,
    active = 1,
    locked = 0,
    updated_at = NOW(),
    updated_by = 'flyway'
WHERE LOWER(TRIM(username)) = 'superadmin'
  AND password_change_required = 1
  AND created_by = 'flyway';

-- If both spellings existed, retain the canonical account and archive legacy rows.
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
