SET @calendar_events_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'calendar_events'
);

SET @calendar_events_rows := (
    SELECT COALESCE(MAX(TABLE_ROWS), 0)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'calendar_events'
);

SET @drop_calendar_events := IF(
    @calendar_events_exists > 0 AND @calendar_events_rows = 0,
    'DROP TABLE calendar_events',
    'SELECT 1'
);

PREPARE stmt FROM @drop_calendar_events;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
