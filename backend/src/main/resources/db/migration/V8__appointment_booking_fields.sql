-- ============================================================
-- MeghaConnect V8: Public/Citizen Module – Appointment Booking Fields
-- MySQL 8 Compatible
-- ============================================================
-- Adds support for:
--   - Application type (NEW_APPLICATION / REMINDER) on appointments
--   - Scheme history list as JSON on appointments
--   - visitor_associates table creation
--   - Address field for visitor associates
-- ============================================================

-- 0. CREATE visitor_associates TABLE (if not exists - was missing from V1-V7)
CREATE TABLE IF NOT EXISTS visitor_associates (
    associate_id         BIGINT       NOT NULL AUTO_INCREMENT,
    primary_visitor_id   BIGINT       NOT NULL,
    full_name            VARCHAR(200) NOT NULL,
    phone_number         VARCHAR(20),
    epic_number          VARCHAR(50),
    designation          VARCHAR(100),
    district             VARCHAR(100),
    photo_path           VARCHAR(200),
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (associate_id),
    CONSTRAINT fk_associate_visitor FOREIGN KEY (primary_visitor_id) REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
