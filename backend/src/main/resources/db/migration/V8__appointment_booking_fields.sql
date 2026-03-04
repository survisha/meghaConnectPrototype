-- ============================================================
-- MeghaConnect V8: Public/Citizen Module – Appointment Booking Fields
-- MySQL 8 Compatible
-- ============================================================
-- Adds support for:
--   - Application type (NEW_APPLICATION / REMINDER) on appointments
--   - Scheme history list as JSON on appointments
--   - Address field for visitor associates
-- ============================================================

-- 1. ADD application_type TO appointments TABLE (idempotent)
SET @dbname = DATABASE();
SET @tablename = 'appointments';
SET @columnname = 'application_type';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' VARCHAR(30) NULL COMMENT ''NEW_APPLICATION or REMINDER''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. ADD scheme_history_json TO appointments TABLE (idempotent)
SET @columnname = 'scheme_history_json';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' TEXT NULL COMMENT ''JSON array of schemes taken in last 2 years''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. ADD address TO visitor_associates TABLE (idempotent)
SET @tablename = 'visitor_associates';
SET @columnname = 'address';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' VARCHAR(500) NULL COMMENT ''Address of associate visitor''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================================
-- NOTES:
-- ============================================================
-- • application_type: Tracks whether the appointment is a New Application
--   or a Reminder for an existing/old application.
--
-- • scheme_history_json: Stores a JSON array of schemes the applicant has
--   received in the last 2 years. Example: ["CMSDF","CMSG"].
--   Only JSON array format should be used; comma-separated values are not supported.
--
-- • address in visitor_associates: Stores address details for each
--   associate who accompanies the primary visitor.
-- ============================================================
