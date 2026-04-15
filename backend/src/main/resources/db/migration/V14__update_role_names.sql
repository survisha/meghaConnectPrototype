-- ============================================================
-- Update Role Names (SAIDUL_OSD → OSD, APPROVER_JT_SECY → APPROVER)
-- ============================================================
-- This migration standardizes role names across the system
-- to shorter, more maintainable values

-- Update any existing users with old role names to new role names
UPDATE users SET role = 'OSD' WHERE role = 'SAIDUL_OSD';
UPDATE users SET role = 'APPROVER' WHERE role = 'APPROVER_JT_SECY';
