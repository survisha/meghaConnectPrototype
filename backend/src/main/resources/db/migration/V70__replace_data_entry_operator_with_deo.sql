-- DEO is the canonical role. Preserve every user account while replacing the
-- obsolete DATA_ENTRY_OPERATOR role code; historical audit strings are left intact.
INSERT INTO roles (role_name, description, created_at)
VALUES ('DEO', 'Department data entry operator', NOW())
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_at = NOW();

UPDATE users
SET role = 'DEO',
    updated_at = NOW(),
    updated_by = 'flyway-v70'
WHERE role = 'DATA_ENTRY_OPERATOR';

DELETE FROM roles
WHERE role_name = 'DATA_ENTRY_OPERATOR';
