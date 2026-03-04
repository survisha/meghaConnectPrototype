-- ============================================================
-- MeghaConnect V9: AI Enabled Smart Governance – DB Fields
-- MySQL 8 Compatible
-- ============================================================
-- Adds AI output storage columns to the appointments table:
--   - ai_summary         : AI-generated document/agenda summary (R005)
--   - ai_extracted_fields: JSON of fields extracted from documents (R004)
--   - ai_priority_level  : HIGH / MEDIUM / LOW recommendation (R007)
--   - ai_duplicate_flag  : Boolean flag for duplicate detection (R006)
-- ============================================================

SET @dbname = DATABASE();
SET @tablename = 'appointments';

-- 1. ai_summary
SET @columnname = 'ai_summary';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' TEXT NULL COMMENT ''AI-generated document summary (R005)''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. ai_extracted_fields
SET @columnname = 'ai_extracted_fields';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' TEXT NULL COMMENT ''JSON of AI-extracted fields from documents (R004)''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. ai_priority_level
SET @columnname = 'ai_priority_level';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' VARCHAR(10) NULL COMMENT ''AI meeting priority: HIGH, MEDIUM, LOW (R007)''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 4. ai_duplicate_flag
SET @columnname = 'ai_duplicate_flag';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname,
         ' TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 if AI detected duplicate application, 0 otherwise (R006)''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================================
-- NOTES:
-- ============================================================
-- All 4 columns are nullable (except ai_duplicate_flag which defaults to 0)
-- so existing appointments are not affected.
-- These fields are populated by the AiController endpoints
-- when a document is uploaded or when a citizen submits an application.
-- ============================================================
