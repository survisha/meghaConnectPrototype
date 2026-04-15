-- V17__create_appointment_type_configs.sql
-- Create table for appointment type configurations
-- Stores configuration for appointment types: A1, A2, A3, A4, B1, B2

CREATE TABLE IF NOT EXISTS appointment_type_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(200) NOT NULL,
    description LONGTEXT,
    type_category VARCHAR(50) NOT NULL,
    
    -- Travel time configuration (Type A1, A2)
    requires_travel BOOLEAN NOT NULL DEFAULT FALSE,
    travel_time_before INT NOT NULL DEFAULT 0,     -- In minutes
    travel_time_after INT NOT NULL DEFAULT 0,      -- In minutes
    block_time_includes BOOLEAN NOT NULL DEFAULT TRUE,  -- Does blocking include travel time?
    
    -- Appointment limit configuration (Type A4)
    has_appointment_limit BOOLEAN NOT NULL DEFAULT FALSE,
    max_appointment_limit INT,
    limit_is_sacrosanct BOOLEAN NOT NULL DEFAULT TRUE,   -- Can be exceeded?
    generate_alerts BOOLEAN NOT NULL DEFAULT FALSE,      -- Alert when exceeded?
    
    -- Type A3 characteristics
    no_travel_time BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    
    -- Audit
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_type_code (type_code),
    INDEX idx_type_category (type_category),
    INDEX idx_display_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default appointment types
INSERT IGNORE INTO appointment_type_configs 
(type_code, type_name, description, type_category, requires_travel, travel_time_before, travel_time_after, block_time_includes, has_appointment_limit, limit_is_sacrosanct, no_travel_time, is_active, display_order, created_by, updated_by)
VALUES
    ('A1', 'Cabinet Meetings / Union Minister Meetings / Media Interaction / Flights', 
     'Cabinet Meetings, Meetings with Union Ministers, Media Interactions, Flight schedules', 
     'INDIVIDUAL', TRUE, 30, 30, TRUE, FALSE, TRUE, FALSE, TRUE, 1, 'SYSTEM', 'SYSTEM'),
    
    ('A2', 'Events / Programmes',
     'Public events, Government programmes, Official functions',
     'INDIVIDUAL', TRUE, 0, 0, TRUE, FALSE, TRUE, FALSE, TRUE, 2, 'SYSTEM', 'SYSTEM'),
    
    ('A3', 'Files Clearing / Birthday Greetings',
     'File clearing by HCM, Birthday greetings, Internal administrative work',
     'INDIVIDUAL', FALSE, 0, 0, FALSE, FALSE, TRUE, TRUE, TRUE, 3, 'SYSTEM', 'SYSTEM'),
    
    ('A4', 'Individual Appointments',
     'Regular individual appointments and meetings',
     'INDIVIDUAL', FALSE, 0, 0, FALSE, TRUE, FALSE, FALSE, TRUE, 4, 'SYSTEM', 'SYSTEM'),
    
    ('B1', 'Public Durbar',
     'Public contact/scheme distribution events - Pre-approved by Approval process',
     'BATCH', FALSE, 0, 0, FALSE, FALSE, TRUE, FALSE, TRUE, 5, 'SYSTEM', 'SYSTEM'),
    
    ('B2', 'Public Walk-in',
     'Public walk-in contact/scheme distribution time - No pre-approval required',
     'BATCH', FALSE, 0, 0, FALSE, FALSE, TRUE, FALSE, TRUE, 6, 'SYSTEM', 'SYSTEM');
