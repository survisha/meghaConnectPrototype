SET @is_locked_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'is_locked'
);

SET @locked_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'locked'
);

SET @merge_lock_state := IF(
    @is_locked_exists > 0 AND @locked_exists > 0,
    'UPDATE users SET locked = CASE WHEN locked = 1 OR is_locked = 1 THEN 1 ELSE 0 END',
    'SELECT 1'
);

PREPARE stmt FROM @merge_lock_state;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_is_locked := IF(
    @is_locked_exists > 0,
    'ALTER TABLE users DROP COLUMN is_locked',
    'SELECT 1'
);

PREPARE stmt FROM @drop_is_locked;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
