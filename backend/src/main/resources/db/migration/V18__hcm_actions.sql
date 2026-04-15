-- ============================================================
-- V18: HCM Actions table for gesture-based appointments
-- ============================================================
-- This migration creates the hcm_actions table to track all HCM
-- gesture-based actions on appointments/meetings:
-- Right Swipe: Accept, Mark Important, Modify
-- Left Swipe: Snooze, Reject

CREATE TABLE IF NOT EXISTS hcm_actions (
    id                          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appointment_id              BIGINT        NOT NULL,
    action_type                 VARCHAR(50)   NOT NULL COMMENT 'ACCEPT, ACCEPT_WITH_CHANGES, MARK_IMPORTANT, SNOOZE, REJECT',
    action_status               VARCHAR(50)   NOT NULL COMMENT 'PENDING, CONFIRMED, COMPLETED',
    gesture_type                VARCHAR(50)   COMMENT 'RIGHT_SWIPE, LEFT_SWIPE',
    
    -- For ACCEPT action
    accepted_date_time          DATETIME,
    
    -- For MARK_IMPORTANT action
    is_important_meeting        BOOLEAN,
    requested_earlier_datetime  DATETIME,
    
    -- For SNOOZE action
    snooze_type                 VARCHAR(50)   COMMENT 'DAYS_7, DAYS_15, DAYS_30, CUSTOM',
    snooze_duration_days        INT,
    snoozed_until               DATETIME,
    
    -- For REJECT action
    is_rejected                 BOOLEAN,
    clarification_requested     TEXT,
    
    -- HCM remarks (can be added for any action)
    hcm_remarks                 TEXT,
    
    -- Original appointment details snapshot
    original_datetime           DATETIME,
    original_location           VARCHAR(255),
    appointment_subject         VARCHAR(300),
    
    -- Audit fields
    created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_hcm_action_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    INDEX idx_appointment (appointment_id),
    INDEX idx_action_type (action_type),
    INDEX idx_action_status (action_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HCM gesture-based actions on appointments/meetings';
