-- ============================================================
-- MeghaConnect Extended Schema (V3)
-- Additional tables for full feature coverage as per
-- SCHEDULING & APPOINTMENT HANDLING + CM SCHEME APP requirements
-- ============================================================

-- DOCUMENT UPLOADS
-- Stores uploaded files: EPIC scans, plans & estimates, bank account
-- forms, hospital docs, org registration certificates, etc.
CREATE TABLE document_uploads (
    id                     BIGSERIAL PRIMARY KEY,
    entity_type            VARCHAR(50)  NOT NULL,  -- APPOINTMENT, SCHEME_APPLICATION, PERSON
    entity_id              BIGINT       NOT NULL,
    document_type          VARCHAR(100) NOT NULL,  -- EPIC_SCAN, PLAN_ESTIMATE, BANK_DETAILS,
                                                   -- HOSPITAL_DOC, ORG_REGISTRATION, APP_LETTER,
                                                   -- MLA_MDC_LETTER, ELIGIBILITY_PROOF, PHOTO
    file_path              VARCHAR(500) NOT NULL,
    original_filename      VARCHAR(255),
    mime_type              VARCHAR(100),
    uploaded_by            VARCHAR(100) NOT NULL,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100)
);
CREATE INDEX idx_doc_entity ON document_uploads(entity_type, entity_id);
CREATE INDEX idx_doc_type   ON document_uploads(document_type);

-- BANK ACCOUNT DETAILS
-- Mandatory for scheme disbursements
CREATE TABLE bank_account_details (
    id                       BIGSERIAL PRIMARY KEY,
    person_id                BIGINT       NOT NULL REFERENCES persons(id),
    scheme_application_id    BIGINT       REFERENCES scheme_applications(id),
    account_holder_name      VARCHAR(200) NOT NULL,
    bank_name                VARCHAR(200) NOT NULL,
    branch_name              VARCHAR(200),
    account_number           VARCHAR(50)  NOT NULL,
    ifsc_code                VARCHAR(20)  NOT NULL,
    is_verified              BOOLEAN DEFAULT FALSE,
    created_at               TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100)
);
CREATE INDEX idx_bank_person ON bank_account_details(person_id);

-- APPOINTMENT DAY LIMITS
-- Configures max appointments per day / location / category / designation
CREATE TABLE appointment_day_limits (
    id                  BIGSERIAL PRIMARY KEY,
    location            VARCHAR(20),   -- SHILLONG, TURA, DELHI, OTHERS; NULL = all locations
    designation_filter  VARCHAR(100),  -- e.g. 'Political Leader'; NULL = all designations
    agenda_type_filter  VARCHAR(10),   -- A4, B1, B2; NULL = all agenda types
    scheme_type_filter  VARCHAR(30),   -- e.g. 'CMSDF'; NULL = all schemes
    max_appointments    INTEGER NOT NULL,
    effective_date      DATE,          -- NULL means applies to every day
    created_by          VARCHAR(100),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(100)
);

-- PRIOR SCHEME HISTORY
-- Tracks previously availed schemes (auto-populated from external DBs or manually entered)
CREATE TABLE prior_scheme_history (
    id                BIGSERIAL PRIMARY KEY,
    person_id         BIGINT      NOT NULL REFERENCES persons(id),
    scheme_type       VARCHAR(30) NOT NULL,  -- CMSDF, CMSG, CM_CARE, CM_CONNECT, CM_ELEVATE, FOCUS_PLUS
    availed_year      SMALLINT,
    amount_availed    NUMERIC(14,2),
    project_name      VARCHAR(300),
    status            VARCHAR(50),
    source            VARCHAR(100),  -- 'MANUAL', 'EXCEL_IMPORT', 'SYSTEM'
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);
CREATE INDEX idx_prior_person ON prior_scheme_history(person_id);
CREATE INDEX idx_prior_scheme ON prior_scheme_history(scheme_type);

-- NOTIFICATION LOG
-- Records WhatsApp / SMS / push notifications sent by the system
CREATE TABLE notification_log (
    id               BIGSERIAL PRIMARY KEY,
    recipient_phone  VARCHAR(20)  NOT NULL,
    channel          VARCHAR(20)  NOT NULL,  -- WHATSAPP, SMS, PUSH
    message_type     VARCHAR(100) NOT NULL,  -- APPOINTMENT_CONFIRMED, CANCELLATION, REMINDER,
                                             -- DEPARTURE_ALERT, OTP, BIRTHDAY_PING
    message_body     TEXT         NOT NULL,
    entity_type      VARCHAR(50),
    entity_id        BIGINT,
    status           VARCHAR(20)  NOT NULL,  -- SENT, FAILED, PENDING
    sent_at          TIMESTAMP,
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL
);
CREATE INDEX idx_notif_phone  ON notification_log(recipient_phone);
CREATE INDEX idx_notif_entity ON notification_log(entity_type, entity_id);
CREATE INDEX idx_notif_status ON notification_log(status);

-- MEETING TIMER LOG
-- Tracks actual start/end times of meetings for delay management
CREATE TABLE meeting_timer_log (
    id                  BIGSERIAL PRIMARY KEY,
    appointment_id      BIGINT NOT NULL REFERENCES appointments(id),
    scheduled_start     TIMESTAMP NOT NULL,
    actual_start        TIMESTAMP,
    actual_end          TIMESTAMP,
    delay_minutes       INTEGER,  -- computed: actual_start - scheduled_start
    timer_started_by    VARCHAR(100),
    timer_ended_by      VARCHAR(100),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP
);
CREATE INDEX idx_timer_appt ON meeting_timer_log(appointment_id);

-- FACE RECOGNITION SOURCES
-- Tracks sources from which facial embeddings are built (app photos + external sources)
CREATE TABLE face_recognition_sources (
    id              BIGSERIAL PRIMARY KEY,
    source_name     VARCHAR(200) NOT NULL,
    source_type     VARCHAR(50)  NOT NULL,  -- APP_PHOTO, EXCEL_IMPORT, EXTERNAL_DB, OTHER
    description     TEXT,
    record_count    INTEGER DEFAULT 0,
    last_synced_at  TIMESTAMP,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- EXTERNAL SCHEME RECORDS
-- Stores records imported from external Excel-based scheme databases
-- (CMSDF, CMSG, CM Care, CM Connect, CM Elevate, Focus+, etc.)
CREATE TABLE external_scheme_records (
    id                   BIGSERIAL PRIMARY KEY,
    scheme_type          VARCHAR(30)  NOT NULL,
    beneficiary_name     VARCHAR(200) NOT NULL,
    phone_number         VARCHAR(20),
    epic_number          VARCHAR(50),
    district             VARCHAR(100),
    constituency         VARCHAR(100),
    booth                VARCHAR(100),
    village              VARCHAR(100),
    project_name         VARCHAR(300),
    project_category     VARCHAR(100),
    amount_sanctioned    NUMERIC(14,2),
    financial_year       VARCHAR(10),
    status               VARCHAR(50),
    source_file          VARCHAR(255),  -- original Excel filename
    import_batch_id      VARCHAR(100),
    matched_person_id    BIGINT REFERENCES persons(id),  -- NULL if not yet matched
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);
CREATE INDEX idx_ext_scheme      ON external_scheme_records(scheme_type);
CREATE INDEX idx_ext_phone       ON external_scheme_records(phone_number);
CREATE INDEX idx_ext_epic        ON external_scheme_records(epic_number);
CREATE INDEX idx_ext_name        ON external_scheme_records(LOWER(beneficiary_name));
CREATE INDEX idx_ext_constituency ON external_scheme_records(constituency);
CREATE INDEX idx_ext_matched     ON external_scheme_records(matched_person_id);

-- APPROVAL DELEGATION LOG
-- Audit trail when Approver (Jt Secy) delegates authority to a CMO officer
CREATE TABLE approval_delegation_log (
    id                   BIGSERIAL PRIMARY KEY,
    delegating_user_id   BIGINT NOT NULL REFERENCES users(id),
    delegated_to_user_id BIGINT NOT NULL REFERENCES users(id),
    valid_from           TIMESTAMP NOT NULL,
    valid_until          TIMESTAMP NOT NULL,  -- max 7 days from valid_from
    reason               TEXT,
    is_revoked           BOOLEAN DEFAULT FALSE,
    revoked_at           TIMESTAMP,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);
CREATE INDEX idx_deleg_from ON approval_delegation_log(delegating_user_id);
CREATE INDEX idx_deleg_to   ON approval_delegation_log(delegated_to_user_id);

-- CONSTITUENCY HEATMAP CACHE
-- Pre-computed constituency-level heatmap data for scheme & appointment analytics
CREATE TABLE constituency_heatmap_cache (
    id                   BIGSERIAL PRIMARY KEY,
    district             VARCHAR(100) NOT NULL,
    constituency         VARCHAR(100) NOT NULL,
    scheme_type          VARCHAR(30),   -- NULL = aggregated across all schemes
    total_applications   INTEGER DEFAULT 0,
    approved_count       INTEGER DEFAULT 0,
    rejected_count       INTEGER DEFAULT 0,
    pending_count        INTEGER DEFAULT 0,
    total_amount         NUMERIC(16,2) DEFAULT 0,
    heat_score           NUMERIC(5,2),  -- 0-100 scale
    last_computed_at     TIMESTAMP NOT NULL,
    period_from          DATE,
    period_to            DATE
);
CREATE INDEX idx_heat_constituency ON constituency_heatmap_cache(constituency);
CREATE INDEX idx_heat_district     ON constituency_heatmap_cache(district);
CREATE INDEX idx_heat_scheme       ON constituency_heatmap_cache(scheme_type);

-- NPP BLOCK/DISTRICT INTERACTION LOG
-- Tracks Saidul OSD popup responses for NPP block/district level interactions (Type A2)
CREATE TABLE npp_interaction_log (
    id                    BIGSERIAL PRIMARY KEY,
    schedule_event_id     BIGINT NOT NULL REFERENCES schedule_events(id),
    level                 VARCHAR(20) NOT NULL,  -- BLOCK, DISTRICT
    location_name         VARCHAR(200),
    duration_minutes      INTEGER,   -- 15, 30, 45, 60
    decided_by            VARCHAR(50),  -- HCM, SAIDUL_OSD
    decision              VARCHAR(20),  -- YES, NO
    npp_interaction_id    BIGINT REFERENCES schedule_events(id),  -- linked new A2 event if yes
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);

-- APPOINTMENT REJECTION HISTORY
-- Full history of rejection / snooze decisions by HCM, CMO, Approver
CREATE TABLE appointment_rejection_history (
    id               BIGSERIAL PRIMARY KEY,
    appointment_id   BIGINT      NOT NULL REFERENCES appointments(id),
    rejected_by_role VARCHAR(50) NOT NULL,  -- HCM, CMO_OFFICER, APPROVER_JT_SECY
    rejected_by_user VARCHAR(100) NOT NULL,
    rejection_type   VARCHAR(30) NOT NULL,  -- REJECTED, SNOOZED, SENT_BACK
    snooze_days      INTEGER,  -- if type = SNOOZED
    remarks          TEXT,
    created_at       TIMESTAMP NOT NULL
);
CREATE INDEX idx_rej_appt ON appointment_rejection_history(appointment_id);
