-- ============================================================
-- MeghaConnect V32: Schedule Event Short Notes
-- MySQL 8 Compatible
-- ============================================================
-- Adds the nullable short_notes column used by the ScheduleEvent entity.
-- The check keeps startup safe if the column was already created during
-- an earlier failed run or by Hibernate schema update.
-- ============================================================

SET @dbname = DATABASE();
SET @tablename = 'schedule_events';
SET @columnname = 'short_notes';

SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TEXT NULL AFTER description')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
