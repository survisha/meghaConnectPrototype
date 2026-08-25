-- Primary database repair for deployments where earlier V77/V78 versions
-- were recorded elsewhere or did not add Appointment close metadata.
-- This file belongs only to classpath:db/migration (meghaconnect_db).

DELIMITER //

CREATE PROCEDURE add_v79_appointment_close_column_if_missing(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'appointments'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE appointments ADD COLUMN ',
            p_column_name,
            ' ',
            p_column_definition
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

DELIMITER ;

CALL add_v79_appointment_close_column_if_missing(
    'closed_by',
    'VARCHAR(100) NULL'
);
CALL add_v79_appointment_close_column_if_missing(
    'closed_at',
    'TIMESTAMP NULL'
);

DROP PROCEDURE add_v79_appointment_close_column_if_missing;
