-- ============================================================
-- MeghaConnect V43: Guest appointments
-- ============================================================

DELIMITER $$

CREATE PROCEDURE add_guest_appointment_column_if_missing(
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'appointments'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE appointments ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_guest_appointment_column_if_missing('appointment_source', 'VARCHAR(20) NULL');
CALL add_guest_appointment_column_if_missing('guest_reference_id', 'VARCHAR(40) NULL');
CALL add_guest_appointment_column_if_missing('guest_name', 'VARCHAR(200) NULL');
CALL add_guest_appointment_column_if_missing('guest_mobile', 'VARCHAR(20) NULL');
CALL add_guest_appointment_column_if_missing('guest_address', 'VARCHAR(500) NULL');
CALL add_guest_appointment_column_if_missing('guest_email', 'VARCHAR(150) NULL');
CALL add_guest_appointment_column_if_missing('organization_name', 'VARCHAR(200) NULL');
CALL add_guest_appointment_column_if_missing('guest_designation', 'VARCHAR(100) NULL');
CALL add_guest_appointment_column_if_missing('visitor_category', 'VARCHAR(100) NULL');
CALL add_guest_appointment_column_if_missing('referred_office', 'VARCHAR(100) NULL');
CALL add_guest_appointment_column_if_missing('referred_by_name', 'VARCHAR(200) NULL');
CALL add_guest_appointment_column_if_missing('reason_for_appointment', 'VARCHAR(500) NULL');
CALL add_guest_appointment_column_if_missing('preferred_date', 'DATE NULL');

UPDATE appointments
SET appointment_source = 'CITIZEN'
WHERE appointment_source IS NULL;

INSERT IGNORE INTO reference_type (code, name, description, status) VALUES
('GUEST_REFERRED_OFFICE', 'Guest Referred Offices', 'Offices/persons that process guest appointment requests', 'ACTIVE'),
('GUEST_VISITOR_CATEGORY', 'Guest Visitor Categories', 'Visitor categories for guest appointments', 'ACTIVE');

INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active)
VALUES
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'OSD', 'OSD', 1, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'JOINT_SECRETARY', 'Joint Secretary', 2, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'CMO_OFFICE', 'CMO Office', 3, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'CM_SECRETARIAT', 'CM Secretariat', 4, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'PERSONAL_SECTION', 'Personal Section', 5, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'PROTOCOL_OFFICE', 'Protocol Office', 6, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_REFERRED_OFFICE'), 'OTHER', 'Other', 7, TRUE);

INSERT IGNORE INTO reference_data (type_id, code, value, display_order, is_active)
VALUES
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'BUSINESS_PERSON', 'Business Person', 1, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'ARMY_PERSONNEL', 'Army Personnel', 2, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'GOVERNMENT_OFFICIAL', 'Government Official', 3, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'PUBLIC_REPRESENTATIVE', 'Public Representative', 4, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'ORGANIZATION_REPRESENTATIVE', 'Organization Representative', 5, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'NGO_REPRESENTATIVE', 'NGO Representative', 6, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'MEDIA', 'Media', 7, TRUE),
((SELECT id FROM reference_type WHERE code = 'GUEST_VISITOR_CATEGORY'), 'OTHER', 'Other', 8, TRUE);

DROP PROCEDURE add_guest_appointment_column_if_missing;
