-- ============================================================
-- MeghaConnect Database Schema – MySQL 8
-- ============================================================

-- USERS
CREATE TABLE users (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    username               VARCHAR(100) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    full_name              VARCHAR(100) NOT NULL,
    role                   VARCHAR(50)  NOT NULL,
    phone_number           VARCHAR(20),
    active                 TINYINT(1)   NOT NULL DEFAULT 1,
    offline_access         TINYINT(1)   NOT NULL DEFAULT 0,
    last_login             DATETIME,
    delegated_to_user_id   BIGINT,
    delegation_expires_at  DATETIME,
    created_at             DATETIME     NOT NULL,
    updated_at             DATETIME,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_user_delegate FOREIGN KEY (delegated_to_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PERSONS (Citizens / Applicants)
CREATE TABLE persons (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    full_name           VARCHAR(200) NOT NULL,
    phone_number        VARCHAR(20),
    epic_number         VARCHAR(50),
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
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_person_phone        ON persons(phone_number);
CREATE INDEX idx_person_epic         ON persons(epic_number);
CREATE INDEX idx_person_name         ON persons(full_name);
CREATE INDEX idx_person_constituency ON persons(constituency);

-- SCHEDULE EVENTS
CREATE TABLE schedule_events (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    title                 VARCHAR(300) NOT NULL,
    event_type            VARCHAR(10)  NOT NULL COMMENT 'A1,A2,A3,A4,B1,B2',
    start_time            DATETIME     NOT NULL,
    end_time              DATETIME     NOT NULL,
    location              VARCHAR(20)  NOT NULL,
    travel_time_minutes   INT,
    description           TEXT,
    is_conflict           TINYINT(1)   NOT NULL DEFAULT 0,
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_event_start ON schedule_events(start_time);

-- APPOINTMENTS
CREATE TABLE appointments (
    id                            BIGINT      NOT NULL AUTO_INCREMENT,
    application_id                VARCHAR(30) NOT NULL UNIQUE,
    applicant_id                  BIGINT      NOT NULL,
    event_type                    VARCHAR(10) NOT NULL,
    agenda_type                   VARCHAR(200),
    agenda_brief                  TEXT,
    status                        VARCHAR(50) NOT NULL,
    requested_location            VARCHAR(20),
    scheduled_date_time           DATETIME,
    scheduled_duration_minutes    INT,
    mla_mdc_approved              TINYINT(1),
    cmo_remarks                   TEXT,
    hcm_remarks                   TEXT,
    is_walk_in                    TINYINT(1)  NOT NULL DEFAULT 0,
    snoozed_until                 DATETIME,
    meeting_count_last6_months    INT         DEFAULT 0,
    schedule_event_id             BIGINT,
    created_at                    DATETIME    NOT NULL,
    updated_at                    DATETIME,
    created_by                    VARCHAR(100),
    updated_by                    VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_appt_person FOREIGN KEY (applicant_id)      REFERENCES persons(id),
    CONSTRAINT fk_appt_event  FOREIGN KEY (schedule_event_id) REFERENCES schedule_events(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_appt_appid     ON appointments(application_id);
CREATE INDEX idx_appt_status    ON appointments(status);
CREATE INDEX idx_appt_applicant ON appointments(applicant_id);
CREATE INDEX idx_appt_scheduled ON appointments(scheduled_date_time);

-- DIRECTIONS (HCM colour-coded directions)
CREATE TABLE directions (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    appointment_id       BIGINT       NOT NULL,
    color                VARCHAR(10)  NOT NULL COMMENT 'GREEN, YELLOW, BLUE',
    direction_text       TEXT         NOT NULL,
    assigned_department  VARCHAR(200),
    assigned_officer     VARCHAR(200),
    deadline             DATE,
    current_status       TEXT,
    completed            TINYINT(1)   NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_dir_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_dir_appt      ON directions(appointment_id);
CREATE INDEX idx_dir_completed ON directions(completed);

-- ASSOCIATE MAPPINGS
CREATE TABLE associate_mappings (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT       NOT NULL,
    person_id      BIGINT       NOT NULL,
    relationship   VARCHAR(200),
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uq_assoc (appointment_id, person_id),
    CONSTRAINT fk_assoc_appt   FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_assoc_person FOREIGN KEY (person_id)      REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SCHEME APPLICATIONS
CREATE TABLE scheme_applications (
    id                      BIGINT         NOT NULL AUTO_INCREMENT,
    applicant_id            BIGINT         NOT NULL,
    appointment_id          BIGINT,
    scheme_type             VARCHAR(30)    NOT NULL,
    project_name            VARCHAR(300)   NOT NULL,
    project_category        VARCHAR(100),
    beneficiary_type        VARCHAR(100),
    beneficiary_count       VARCHAR(50),
    estimated_cost          DECIMAL(14,2),
    community_contribution  DECIMAL(14,2),
    justification           TEXT,
    cmo_moderated_cost      DECIMAL(14,2),
    hcm_decision            VARCHAR(30),
    hcm_approved_cost       DECIMAL(14,2),
    hcm_remarks             TEXT,
    status                  VARCHAR(50),
    created_at              DATETIME       NOT NULL,
    updated_at              DATETIME,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_scheme_person FOREIGN KEY (applicant_id)   REFERENCES persons(id),
    CONSTRAINT fk_scheme_appt   FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_scheme_applicant ON scheme_applications(applicant_id);
CREATE INDEX idx_scheme_type      ON scheme_applications(scheme_type);
CREATE INDEX idx_scheme_status    ON scheme_applications(status);

-- SCHEME APPLICATION ITEMS
CREATE TABLE scheme_application_items (
    id                          BIGINT         NOT NULL AUTO_INCREMENT,
    scheme_application_id       BIGINT         NOT NULL,
    description                 VARCHAR(300)   NOT NULL,
    quantity                    INT            NOT NULL DEFAULT 1,
    unit_cost                   DECIMAL(14,2)  NOT NULL,
    cmo_moderated_unit_cost     DECIMAL(14,2),
    hcm_approved_unit_cost      DECIMAL(14,2),
    PRIMARY KEY (id),
    CONSTRAINT fk_item_scheme FOREIGN KEY (scheme_application_id) REFERENCES scheme_applications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AUDIT LOGS
CREATE TABLE audit_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     BIGINT,
    action        VARCHAR(100) NOT NULL,
    details       TEXT,
    performed_by  VARCHAR(100) NOT NULL,
    timestamp     DATETIME     NOT NULL,
    ip_address    VARCHAR(50),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user   ON audit_logs(performed_by);
CREATE INDEX idx_audit_time   ON audit_logs(timestamp);
