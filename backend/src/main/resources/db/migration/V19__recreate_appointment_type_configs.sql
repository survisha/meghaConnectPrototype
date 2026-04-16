-- V19__recreate_appointment_type_configs.sql
-- Drop and recreate appointment_type_configs table with all required columns

DROP TABLE IF EXISTS appointment_type_configs;

CREATE TABLE appointment_type_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(200) NOT NULL,
    description LONGTEXT,
    type_category VARCHAR(50) NOT NULL,
    
    -- Travel time configuration (Type A1, A2)
    requires_travel BOOLEAN NOT NULL DEFAULT FALSE,
    travel_time_before INT NOT NULL DEFAULT 0,
    travel_time_after INT NOT NULL DEFAULT 0,
    block_time_includes BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Appointment limit configuration (Type A4)
    has_appointment_limit BOOLEAN NOT NULL DEFAULT FALSE,
    max_appointment_limit INT,
    limit_is_sacrosanct BOOLEAN NOT NULL DEFAULT TRUE,
    generate_alerts BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Type A3 characteristics
    no_travel_time BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Calendar configuration
    calendar_color VARCHAR(50),
    block_calendar_slot BOOLEAN NOT NULL DEFAULT TRUE,
    availability_windows VARCHAR(500),
    blackout_dates LONGTEXT,
    
    -- Direct scheduling rules
    allow_direct_scheduling BOOLEAN NOT NULL DEFAULT FALSE,
    direct_scheduling_roles VARCHAR(500),
    bypass_approval_process BOOLEAN NOT NULL DEFAULT FALSE,
    approver_bypass_roles VARCHAR(500),
    
    -- Conflict handling
    detect_conflicts BOOLEAN NOT NULL DEFAULT TRUE,
    allow_conflict_override BOOLEAN NOT NULL DEFAULT FALSE,
    conflict_override_roles VARCHAR(500),
    notify_on_conflict BOOLEAN NOT NULL DEFAULT TRUE,
    conflict_notification_template LONGTEXT,
    
    -- Batch scheduling (Type B)
    is_batch_type BOOLEAN NOT NULL DEFAULT FALSE,
    max_participants INT,
    requires_pre_approval BOOLEAN NOT NULL DEFAULT FALSE,
    allow_walk_in BOOLEAN NOT NULL DEFAULT FALSE,
    max_walk_in_count INT,
    auto_assign_seat BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Waiting list management
    enable_waiting_list BOOLEAN NOT NULL DEFAULT FALSE,
    max_waiting_list_size INT,
    auto_add_from_waiting_list BOOLEAN NOT NULL DEFAULT FALSE,
    waiting_list_notification_days INT,
    
    -- OSD special role
    osd_can_override BOOLEAN NOT NULL DEFAULT FALSE,
    osd_can_bypass_limits BOOLEAN NOT NULL DEFAULT FALSE,
    osd_can_direct_schedule BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Drag and drop scheduling
    allow_drag_drop_rescheduling BOOLEAN NOT NULL DEFAULT FALSE,
    drag_drop_allowed_roles VARCHAR(500),
    validate_conflicts_on_drag_drop BOOLEAN NOT NULL DEFAULT TRUE,
    
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
INSERT INTO appointment_type_configs 
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
