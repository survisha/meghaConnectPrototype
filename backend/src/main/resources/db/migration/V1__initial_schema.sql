-- ============================================================
-- MeghaConnect Database Schema
-- ============================================================

-- USERS
CREATE TABLE users (
    id                     BIGSERIAL PRIMARY KEY,
    username               VARCHAR(100) UNIQUE NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    full_name              VARCHAR(100) NOT NULL,
    role                   VARCHAR(50)  NOT NULL,
    phone_number           VARCHAR(20),
    active                 BOOLEAN DEFAULT TRUE,
    offline_access         BOOLEAN DEFAULT FALSE,
    last_login             TIMESTAMP,
    delegated_to_user_id   BIGINT REFERENCES users(id),
    delegation_expires_at  TIMESTAMP,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100)
);

-- PERSONS (Citizens / Applicants)
CREATE TABLE persons (
    id                  BIGSERIAL PRIMARY KEY,
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
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX idx_person_phone ON persons(phone_number);
CREATE INDEX idx_person_epic  ON persons(epic_number);
CREATE INDEX idx_person_name  ON persons(LOWER(full_name));
CREATE INDEX idx_person_constituency ON persons(constituency);

-- SCHEDULE EVENTS
CREATE TABLE schedule_events (
    id                    BIGSERIAL PRIMARY KEY,
    title                 VARCHAR(300) NOT NULL,
    event_type            VARCHAR(10)  NOT NULL,  -- A1,A2,A3,A4,B1,B2
    start_time            TIMESTAMP    NOT NULL,
    end_time              TIMESTAMP    NOT NULL,
    location              VARCHAR(20)  NOT NULL,
    travel_time_minutes   INTEGER,
    description           TEXT,
    is_conflict           BOOLEAN DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);
CREATE INDEX idx_event_start ON schedule_events(start_time);

-- APPOINTMENTS
CREATE TABLE appointments (
    id                            BIGSERIAL PRIMARY KEY,
    application_id                VARCHAR(30) UNIQUE NOT NULL,
    applicant_id                  BIGINT NOT NULL REFERENCES persons(id),
    event_type                    VARCHAR(10) NOT NULL,
    agenda_type                   VARCHAR(200),
    agenda_brief                  TEXT,
    status                        VARCHAR(50) NOT NULL,
    requested_location            VARCHAR(20),
    scheduled_date_time           TIMESTAMP,
    scheduled_duration_minutes    INTEGER,
    mla_mdc_approved              BOOLEAN,
    cmo_remarks                   TEXT,
    hcm_remarks                   TEXT,
    is_walk_in                    BOOLEAN DEFAULT FALSE,
    snoozed_until                 TIMESTAMP,
    meeting_count_last6_months    INTEGER DEFAULT 0,
    schedule_event_id             BIGINT REFERENCES schedule_events(id),
    created_at                    TIMESTAMP NOT NULL,
    updated_at                    TIMESTAMP,
    created_by                    VARCHAR(100),
    updated_by                    VARCHAR(100)
);
CREATE INDEX idx_appt_appid    ON appointments(application_id);
CREATE INDEX idx_appt_status   ON appointments(status);
CREATE INDEX idx_appt_applicant ON appointments(applicant_id);
CREATE INDEX idx_appt_scheduled ON appointments(scheduled_date_time);

-- DIRECTIONS (HCM colour-coded directions)
CREATE TABLE directions (
    id                   BIGSERIAL PRIMARY KEY,
    appointment_id       BIGINT NOT NULL REFERENCES appointments(id),
    color                VARCHAR(10) NOT NULL,  -- GREEN, YELLOW, BLUE
    direction_text       TEXT NOT NULL,
    assigned_department  VARCHAR(200),
    assigned_officer     VARCHAR(200),
    deadline             DATE,
    current_status       TEXT,
    completed            BOOLEAN DEFAULT FALSE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);
CREATE INDEX idx_dir_appt ON directions(appointment_id);
CREATE INDEX idx_dir_completed ON directions(completed);

-- ASSOCIATE MAPPINGS
CREATE TABLE associate_mappings (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id),
    person_id      BIGINT NOT NULL REFERENCES persons(id),
    relationship   VARCHAR(200),
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    UNIQUE(appointment_id, person_id)
);

-- SCHEME APPLICATIONS
CREATE TABLE scheme_applications (
    id                      BIGSERIAL PRIMARY KEY,
    applicant_id            BIGINT NOT NULL REFERENCES persons(id),
    appointment_id          BIGINT REFERENCES appointments(id),
    scheme_type             VARCHAR(30) NOT NULL,
    project_name            VARCHAR(300) NOT NULL,
    project_category        VARCHAR(100),
    beneficiary_type        VARCHAR(100),
    beneficiary_count       VARCHAR(50),
    estimated_cost          NUMERIC(14,2),
    community_contribution  NUMERIC(14,2),
    justification           TEXT,
    cmo_moderated_cost      NUMERIC(14,2),
    hcm_decision            VARCHAR(30),
    hcm_approved_cost       NUMERIC(14,2),
    hcm_remarks             TEXT,
    status                  VARCHAR(50),
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);
CREATE INDEX idx_scheme_applicant ON scheme_applications(applicant_id);
CREATE INDEX idx_scheme_type      ON scheme_applications(scheme_type);
CREATE INDEX idx_scheme_status    ON scheme_applications(status);

-- SCHEME APPLICATION ITEMS (itemwise cost breakdown)
CREATE TABLE scheme_application_items (
    id                          BIGSERIAL PRIMARY KEY,
    scheme_application_id       BIGINT NOT NULL REFERENCES scheme_applications(id) ON DELETE CASCADE,
    description                 VARCHAR(300) NOT NULL,
    quantity                    INTEGER NOT NULL DEFAULT 1,
    unit_cost                   NUMERIC(14,2) NOT NULL,
    cmo_moderated_unit_cost     NUMERIC(14,2),
    hcm_approved_unit_cost      NUMERIC(14,2)
);

-- AUDIT LOGS
CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     BIGINT,
    action        VARCHAR(100) NOT NULL,
    details       TEXT,
    performed_by  VARCHAR(100) NOT NULL,
    timestamp     TIMESTAMP NOT NULL,
    ip_address    VARCHAR(50)
);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user   ON audit_logs(performed_by);
CREATE INDEX idx_audit_time   ON audit_logs(timestamp);
