-- ============================================================
-- MeghaConnect Database Schema
-- MySQL 8 Compatible
-- Database: meghaconnect_db
-- Engine: InnoDB
-- Project: MeghaConnect – Meghalaya Entry & Governance System
-- Company: Survisha Technologies
-- ============================================================
-- Setup:
--   mysql -u root -p < database/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS meghaconnect_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE meghaconnect_db;

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    role_id      BIGINT         NOT NULL AUTO_INCREMENT,
    role_name    VARCHAR(50)    NOT NULL UNIQUE,
    description  TEXT,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- USERS (Staff Accounts)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    username              VARCHAR(100)  NOT NULL UNIQUE,
    password_hash         VARCHAR(255)  NOT NULL,
    full_name             VARCHAR(100)  NOT NULL,
    role                  VARCHAR(50)   NOT NULL,
    phone_number          VARCHAR(20),
    active                TINYINT(1)    NOT NULL DEFAULT 1,
    offline_access        TINYINT(1)    NOT NULL DEFAULT 0,
    failed_attempts       INT           NOT NULL DEFAULT 0,
    is_locked             TINYINT(1)    NOT NULL DEFAULT 0,
    last_login            DATETIME,
    delegated_to_user_id  BIGINT,
    delegation_expires_at DATETIME,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_user_delegate FOREIGN KEY (delegated_to_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_mobile ON users(phone_number);

-- ============================================================
-- VISITORS (visitors / citizens / applicants)
-- ============================================================
CREATE TABLE IF NOT EXISTS visitors (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    full_name           VARCHAR(200)  NOT NULL,
    phone_number        VARCHAR(20),
    epic_number         VARCHAR(50),
    aadhaar_number      VARCHAR(20),
    kyc_type            VARCHAR(10)   COMMENT 'EPIC, AADHAR, or NONE',
    kyc_verified        TINYINT(1)    DEFAULT 0,
    kyc_verified_at     DATETIME,
    photo_storage_path  VARCHAR(500),
    photo_path          VARCHAR(200),
    designation         VARCHAR(100),
    district            VARCHAR(100),
    constituency        VARCHAR(100),
    booth               VARCHAR(100),
    village             VARCHAR(100),
    brief_profile       TEXT,
    date_of_birth       DATE,
    address             VARCHAR(500),
    face_embedding_ref  VARCHAR(500),
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_visitor_id      ON visitors(id);
CREATE INDEX idx_visitor_mobile   ON visitors(phone_number);
CREATE INDEX idx_visitor_epic     ON visitors(epic_number);
CREATE INDEX idx_visitor_name     ON visitors(full_name);

-- ============================================================
-- VISITOR ASSOCIATES
-- ============================================================
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
    CONSTRAINT fk_associate_visitor FOREIGN KEY (primary_visitor_id) REFERENCES visitors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SCHEMES
-- ============================================================
CREATE TABLE IF NOT EXISTS schemes (
    scheme_id    BIGINT       NOT NULL AUTO_INCREMENT,
    scheme_code  VARCHAR(50)  NOT NULL UNIQUE,
    scheme_name  VARCHAR(200) NOT NULL,
    description  TEXT,
    active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (scheme_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_scheme_id ON schemes(scheme_id);

-- ============================================================
-- SCHEME APPLICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS scheme_applications (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    applicant_id          BIGINT         NOT NULL COMMENT 'FK to visitors',
    appointment_id        BIGINT         COMMENT 'FK to appointments (optional)',
    scheme_type           VARCHAR(50)    NOT NULL,
    project_name          VARCHAR(300)   NOT NULL,
    project_category      VARCHAR(100),
    beneficiary_type      VARCHAR(100),
    beneficiary_count     VARCHAR(50),
    estimated_cost        DECIMAL(14,2),
    community_contribution DECIMAL(14,2),
    justification         TEXT,
    cmo_moderated_cost    DECIMAL(14,2),
    hcm_decision          VARCHAR(30),
    hcm_approved_cost     DECIMAL(14,2),
    hcm_remarks           TEXT,
    status                VARCHAR(50),
    ai_summary            TEXT,
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_schemeapp_visitor FOREIGN KEY (applicant_id) REFERENCES visitors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_scheme_id_app    ON scheme_applications(id);
CREATE INDEX idx_schemeapp_visitor ON scheme_applications(applicant_id);

-- ============================================================
-- SCHEME APPLICATION ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS scheme_application_items (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    scheme_application_id BIGINT       NOT NULL,
    item_name           VARCHAR(200)   NOT NULL,
    quantity            INT,
    unit_cost           DECIMAL(14,2),
    total_cost          DECIMAL(14,2),
    remarks             TEXT,
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_item_schemeapp FOREIGN KEY (scheme_application_id) REFERENCES scheme_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- APPOINTMENT TYPES
-- ============================================================
CREATE TABLE IF NOT EXISTS appointment_types (
    type_id      BIGINT      NOT NULL AUTO_INCREMENT,
    type_code    VARCHAR(10) NOT NULL UNIQUE,
    description  VARCHAR(200),
    is_batch     TINYINT(1)  NOT NULL DEFAULT 0,
    max_capacity INT,
    PRIMARY KEY (type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- APPOINTMENT BATCHES
-- ============================================================
CREATE TABLE IF NOT EXISTS appointment_batches (
    batch_id       BIGINT      NOT NULL AUTO_INCREMENT,
    location       VARCHAR(20),
    start_datetime DATETIME,
    end_datetime   DATETIME,
    type_code      VARCHAR(10),
    max_capacity   INT         DEFAULT 15,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SCHEDULE EVENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_events (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    title               VARCHAR(300) NOT NULL,
    event_type          VARCHAR(10)  NOT NULL COMMENT 'A1,A2,A3,A4,B1,B2',
    start_time          DATETIME     NOT NULL,
    end_time            DATETIME     NOT NULL,
    location            VARCHAR(20)  NOT NULL,
    travel_time_minutes INT,
    description         TEXT,
    short_notes         TEXT,
    is_conflict         TINYINT(1)   NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_event_scheduled_date ON schedule_events(start_time);

-- ============================================================
-- APPOINTMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS appointments (
    id                          BIGINT      NOT NULL AUTO_INCREMENT,
    application_id              VARCHAR(30) NOT NULL UNIQUE,
    applicant_id                BIGINT      NOT NULL,
    event_type                  VARCHAR(10) NOT NULL,
    agenda_type                 VARCHAR(200),
    agenda_brief                TEXT,
    status                      VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    requested_location          VARCHAR(20),
    scheduled_date_time         DATETIME,
    scheduled_duration_minutes  INT,
    mla_mdc_approved            TINYINT(1)  DEFAULT 0,
    cmo_remarks                 TEXT,
    approver_remarks            TEXT,
    hcm_remarks                 TEXT,
    short_notes                 TEXT,
    is_walk_in                  TINYINT(1)  NOT NULL DEFAULT 0,
    snoozed_until               DATETIME,
    meeting_count_last6_months  INT         DEFAULT 0,
    schedule_event_id           BIGINT,
    batch_id                    BIGINT,
    created_at                  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_appt_visitor    FOREIGN KEY (applicant_id)    REFERENCES visitors(id),
    CONSTRAINT fk_appt_event     FOREIGN KEY (schedule_event_id) REFERENCES schedule_events(id),
    CONSTRAINT fk_appt_batch     FOREIGN KEY (batch_id)        REFERENCES appointment_batches(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_appointment_id     ON appointments(id);
CREATE INDEX idx_appt_appid         ON appointments(application_id);
CREATE INDEX idx_appt_visitor       ON appointments(applicant_id);
CREATE INDEX idx_appt_scheduled_date ON appointments(scheduled_date_time);

-- ============================================================
-- APPOINTMENT APPROVALS
-- ============================================================
CREATE TABLE IF NOT EXISTS appointment_approvals (
    approval_id    BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT      NOT NULL,
    approver_id    BIGINT      NOT NULL,
    approval_stage VARCHAR(50),
    decision       VARCHAR(20),
    remarks        TEXT,
    decided_at     DATETIME,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (approval_id),
    CONSTRAINT fk_approval_appt     FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_approval_approver FOREIGN KEY (approver_id)    REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- DIRECTIONS (HCM colour-coded directives)
-- ============================================================
CREATE TABLE IF NOT EXISTS directions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_id      BIGINT      NOT NULL,
    color               ENUM('GREEN','YELLOW','BLUE') NOT NULL,
    direction_text      TEXT        NOT NULL,
    assigned_department VARCHAR(200),
    deadline            DATE,
    current_status      TEXT,
    is_completed        TINYINT(1)  NOT NULL DEFAULT 0,
    created_by          VARCHAR(100),
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_direction_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- ASSOCIATE MAPPINGS (companions attending an appointment)
-- ============================================================
CREATE TABLE IF NOT EXISTS associate_mappings (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT      NOT NULL,
    person_id      BIGINT      NOT NULL,
    role_label     VARCHAR(100),
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_assoc_appt   FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_assoc_visitor FOREIGN KEY (person_id)      REFERENCES visitors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- VISITOR PASSES (QR Passes)
-- ============================================================
CREATE TABLE IF NOT EXISTS visitor_passes (
    pass_id        BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT      NOT NULL,
    qr_code_value  VARCHAR(200) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    entry_time     DATETIME,
    exit_time      DATETIME,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (pass_id),
    CONSTRAINT fk_pass_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- CALENDAR EVENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS calendar_events (
    event_id       BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT,
    title          VARCHAR(300) NOT NULL,
    short_notes    TEXT,
    event_date     DATE        NOT NULL,
    start_time     TIME,
    end_time       TIME,
    ics_uid        VARCHAR(200),
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    CONSTRAINT fk_cal_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cal_scheduled_date ON calendar_events(event_date);

-- ============================================================
-- DOCUMENT UPLOADS
-- ============================================================
CREATE TABLE IF NOT EXISTS document_uploads (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    person_id        BIGINT,
    appointment_id   BIGINT,
    scheme_app_id    BIGINT,
    document_type    VARCHAR(100) NOT NULL,
    original_filename VARCHAR(300),
    file_path        VARCHAR(500) NOT NULL,
    file_size_bytes  BIGINT,
    mime_type        VARCHAR(100),
    uploaded_by      VARCHAR(100),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_doc_person    FOREIGN KEY (person_id)      REFERENCES persons(id),
    CONSTRAINT fk_doc_appt      FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_doc_schemeapp FOREIGN KEY (scheme_app_id)  REFERENCES scheme_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_appointment_id_doc ON document_uploads(appointment_id);

-- ============================================================
-- PUBLIC REGISTRATIONS (citizen self-registration via OTP)
-- ============================================================
CREATE TABLE IF NOT EXISTS public_registrations (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    person_id         BIGINT,
    mobile_number     VARCHAR(20)  NOT NULL,
    otp_hash          VARCHAR(255),
    otp_expiry        DATETIME,
    otp_verified      TINYINT(1)   NOT NULL DEFAULT 0,
    kyc_type          VARCHAR(10),
    epic_number       VARCHAR(50),
    aadhaar_number    VARCHAR(20),
    photo_storage_path VARCHAR(500),
    epic_scan_path    VARCHAR(500),
    aadhaar_scan_path VARCHAR(500),
    registration_status VARCHAR(30) NOT NULL DEFAULT 'OTP_PENDING',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_pubreg_person FOREIGN KEY (person_id) REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_mobile_number ON public_registrations(mobile_number);

-- ============================================================
-- KYC VERIFICATION LOG
-- ============================================================
CREATE TABLE IF NOT EXISTS kyc_verification_log (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    person_id       BIGINT,
    reg_id          BIGINT,
    kyc_type        VARCHAR(10) NOT NULL,
    api_endpoint    VARCHAR(300),
    request_ref     VARCHAR(100),
    response_code   VARCHAR(20),
    response_status VARCHAR(50),
    verified        TINYINT(1)  NOT NULL DEFAULT 0,
    attempted_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- GRIEVANCES
-- ============================================================
CREATE TABLE IF NOT EXISTS grievances (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id             VARCHAR(30)  NOT NULL UNIQUE,
    visitor_id            BIGINT,
    applicant_name        VARCHAR(200) NOT NULL,
    phone_number          VARCHAR(20),
    district              VARCHAR(100),
    constituency          VARCHAR(100),
    category              VARCHAR(50)  NOT NULL,
    subject               VARCHAR(300) NOT NULL,
    description           TEXT         NOT NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED',
    assigned_department   VARCHAR(200),
    remarks               TEXT,
    submitted_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at           DATETIME,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_grievance_visitor FOREIGN KEY (visitor_id) REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- AUDIT LOGS (Immutable – no UPDATE/DELETE)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT,
    username     VARCHAR(100),
    action_type  VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT,
    description  TEXT,
    old_value    JSON,
    new_value    JSON,
    ip_address   VARCHAR(50),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- NOTIFICATION LOG
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_log (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    recipient_type  VARCHAR(20),
    recipient_id    BIGINT,
    channel         VARCHAR(20) COMMENT 'SMS, WHATSAPP, EMAIL, PUSH',
    message         TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
    sent_at         DATETIME,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- APPOINTMENT DAY LIMITS
-- ============================================================
CREATE TABLE IF NOT EXISTS appointment_day_limits (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    location    VARCHAR(20) NOT NULL,
    event_type  VARCHAR(10) NOT NULL,
    max_per_day INT         NOT NULL,
    active      TINYINT(1)  NOT NULL DEFAULT 1,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_limit (location, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- CONSTITUENCY HEATMAP CACHE
-- ============================================================
CREATE TABLE IF NOT EXISTS constituency_heatmap_cache (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    district        VARCHAR(100) NOT NULL,
    constituency    VARCHAR(100),
    scheme_count    INT          DEFAULT 0,
    approved_amount DECIMAL(16,2) DEFAULT 0,
    heat_score      DECIMAL(5,2) DEFAULT 0,
    computed_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_heatmap (district, constituency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- FLYWAY SCHEMA HISTORY (managed automatically by Flyway)
-- Shown here for documentation only – do NOT manually insert
-- ============================================================

-- ============================================================
-- SEED DATA
-- ============================================================

-- Roles
INSERT IGNORE INTO roles (role_name, description) VALUES
    ('HCM',                 'Hon. Chief Minister – Full access'),
    ('ADMIN',               'System Administrator – Full system access'),
    ('SAIDUL_OSD',          'OSD to CM – Schedule and scheme management'),
    ('APPROVER_JT_SECY',    'Joint Secretary – Approval workflow'),
    ('CMO_OFFICER',         'CMO Officer – First-line review'),
    ('DATA_ENTRY_OPERATOR', 'Data Entry Operator – Walk-in counter'),
    ('PUBLIC',              'Public Visitor – Own records only');

-- Appointment Types
INSERT IGNORE INTO appointment_types (type_code, description, is_batch, max_capacity) VALUES
    ('A1', 'Cabinet / Union Minister / Media / Flight – High-priority blocked time', 0, NULL),
    ('A2', 'Event / Programme – Scheduled public or official events',                0, NULL),
    ('A3', 'File Clearing / Birthday – Administrative / personal blocks',            0, NULL),
    ('A4', 'Individual Appointment – One-on-one citizen/official meeting',           0, NULL),
    ('B1', 'Public Durbar – Batch public meeting (≤15 per session)',                 1, 15),
    ('B2', 'Public Walk-in – Open walk-in counter session',                          1, NULL);

-- Day Limits (as per SRS: 10/day Shillong, 20/day Tura for A4)
INSERT IGNORE INTO appointment_day_limits (location, event_type, max_per_day) VALUES
    ('SHILLONG', 'A4', 10),
    ('TURA',     'A4', 20);
