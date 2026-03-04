-- ============================================================
-- MeghaConnect Extended Schema (V3) – MySQL 8
-- ============================================================

-- DOCUMENT UPLOADS
CREATE TABLE IF NOT EXISTS document_uploads (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type            VARCHAR(50)  NOT NULL COMMENT 'APPOINTMENT, SCHEME_APPLICATION, PERSON',
    entity_id              BIGINT       NOT NULL,
    document_type          VARCHAR(100) NOT NULL,
    file_path              VARCHAR(500) NOT NULL,
    original_filename      VARCHAR(255),
    mime_type              VARCHAR(100),
    uploaded_by            VARCHAR(100) NOT NULL,
    created_at             DATETIME     NOT NULL,
    updated_at             DATETIME,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_doc_entity ON document_uploads(entity_type, entity_id);
-- CREATE INDEX IF NOT EXISTS idx_doc_type   ON document_uploads(document_type);

-- BANK ACCOUNT DETAILS
CREATE TABLE IF NOT EXISTS bank_account_details (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    person_id                BIGINT       NOT NULL,
    scheme_application_id    BIGINT,
    account_holder_name      VARCHAR(200) NOT NULL,
    bank_name                VARCHAR(200) NOT NULL,
    branch_name              VARCHAR(200),
    account_number           VARCHAR(50)  NOT NULL,
    ifsc_code                VARCHAR(20)  NOT NULL,
    is_verified              TINYINT(1)   NOT NULL DEFAULT 0,
    created_at               DATETIME     NOT NULL,
    updated_at               DATETIME,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_bank_person FOREIGN KEY (person_id) REFERENCES persons(id),
    CONSTRAINT fk_bank_scheme FOREIGN KEY (scheme_application_id) REFERENCES scheme_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_bank_person ON bank_account_details(person_id);

-- APPOINTMENT DAY LIMITS
CREATE TABLE IF NOT EXISTS appointment_day_limits (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    location            VARCHAR(20)  COMMENT 'SHILLONG, TURA, DELHI, OTHERS',
    designation_filter  VARCHAR(100),
    agenda_type_filter  VARCHAR(10),
    scheme_type_filter  VARCHAR(30),
    max_appointments    INT          NOT NULL,
    effective_date      DATE,
    created_by          VARCHAR(100),
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    updated_by          VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PRIOR SCHEME HISTORY
CREATE TABLE IF NOT EXISTS prior_scheme_history (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    person_id         BIGINT         NOT NULL,
    scheme_type       VARCHAR(30)    NOT NULL,
    availed_year      SMALLINT,
    amount_availed    DECIMAL(14,2),
    project_name      VARCHAR(300),
    status            VARCHAR(50),
    source            VARCHAR(100),
    created_at        DATETIME       NOT NULL,
    updated_at        DATETIME,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_prior_person FOREIGN KEY (person_id) REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_prior_person ON prior_scheme_history(person_id);
-- CREATE INDEX IF NOT EXISTS idx_prior_scheme ON prior_scheme_history(scheme_type);

-- NOTIFICATION LOG
CREATE TABLE IF NOT EXISTS notification_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_phone  VARCHAR(20)  NOT NULL,
    channel          VARCHAR(20)  NOT NULL COMMENT 'WHATSAPP, SMS, PUSH',
    message_type     VARCHAR(100) NOT NULL,
    message_body     TEXT         NOT NULL,
    entity_type      VARCHAR(50),
    entity_id        BIGINT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    sent_at          DATETIME,
    error_message    TEXT,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_notif_phone  ON notification_log(recipient_phone);
-- CREATE INDEX IF NOT EXISTS idx_notif_entity ON notification_log(entity_type, entity_id);
-- CREATE INDEX IF NOT EXISTS idx_notif_status ON notification_log(status);

-- MEETING TIMER LOG
CREATE TABLE IF NOT EXISTS meeting_timer_log (
    id                  BIGINT    NOT NULL AUTO_INCREMENT,
    appointment_id      BIGINT    NOT NULL,
    scheduled_start     DATETIME  NOT NULL,
    actual_start        DATETIME,
    actual_end          DATETIME,
    delay_minutes       INT,
    timer_started_by    VARCHAR(100),
    timer_ended_by      VARCHAR(100),
    created_at          DATETIME  NOT NULL,
    updated_at          DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_timer_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_timer_appt ON meeting_timer_log(appointment_id);

-- FACE RECOGNITION SOURCES
CREATE TABLE IF NOT EXISTS face_recognition_sources (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    source_name     VARCHAR(200) NOT NULL,
    source_type     VARCHAR(50)  NOT NULL COMMENT 'APP_PHOTO, EXCEL_IMPORT, EXTERNAL_DB, OTHER',
    description     TEXT,
    record_count    INT          DEFAULT 0,
    last_synced_at  DATETIME,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- EXTERNAL SCHEME RECORDS
CREATE TABLE IF NOT EXISTS external_scheme_records (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    scheme_type          VARCHAR(30)    NOT NULL,
    beneficiary_name     VARCHAR(200)   NOT NULL,
    phone_number         VARCHAR(20),
    epic_number          VARCHAR(50),
    district             VARCHAR(100),
    constituency         VARCHAR(100),
    booth                VARCHAR(100),
    village              VARCHAR(100),
    project_name         VARCHAR(300),
    project_category     VARCHAR(100),
    amount_sanctioned    DECIMAL(14,2),
    financial_year       VARCHAR(10),
    status               VARCHAR(50),
    source_file          VARCHAR(255),
    import_batch_id      VARCHAR(100),
    matched_person_id    BIGINT,
    created_at           DATETIME       NOT NULL,
    updated_at           DATETIME,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_ext_person FOREIGN KEY (matched_person_id) REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_ext_scheme       ON external_scheme_records(scheme_type);
-- CREATE INDEX IF NOT EXISTS idx_ext_phone        ON external_scheme_records(phone_number);
-- CREATE INDEX IF NOT EXISTS idx_ext_epic         ON external_scheme_records(epic_number);
-- CREATE INDEX IF NOT EXISTS idx_ext_name         ON external_scheme_records(beneficiary_name);
-- CREATE INDEX IF NOT EXISTS idx_ext_constituency ON external_scheme_records(constituency);
-- CREATE INDEX IF NOT EXISTS idx_ext_matched      ON external_scheme_records(matched_person_id);

-- APPROVAL DELEGATION LOG
CREATE TABLE IF NOT EXISTS approval_delegation_log (
    id                   BIGINT    NOT NULL AUTO_INCREMENT,
    delegating_user_id   BIGINT    NOT NULL,
    delegated_to_user_id BIGINT    NOT NULL,
    valid_from           DATETIME  NOT NULL,
    valid_until          DATETIME  NOT NULL,
    reason               TEXT,
    is_revoked           TINYINT(1) NOT NULL DEFAULT 0,
    revoked_at           DATETIME,
    created_at           DATETIME  NOT NULL,
    updated_at           DATETIME,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_deleg_from FOREIGN KEY (delegating_user_id)   REFERENCES users(id),
    CONSTRAINT fk_deleg_to   FOREIGN KEY (delegated_to_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_deleg_from ON approval_delegation_log(delegating_user_id);
-- CREATE INDEX IF NOT EXISTS idx_deleg_to   ON approval_delegation_log(delegated_to_user_id);

-- CONSTITUENCY HEATMAP CACHE
CREATE TABLE IF NOT EXISTS constituency_heatmap_cache (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    district             VARCHAR(100)   NOT NULL,
    constituency         VARCHAR(100)   NOT NULL,
    scheme_type          VARCHAR(30),
    total_applications   INT            DEFAULT 0,
    approved_count       INT            DEFAULT 0,
    rejected_count       INT            DEFAULT 0,
    pending_count        INT            DEFAULT 0,
    total_amount         DECIMAL(16,2)  DEFAULT 0,
    heat_score           DECIMAL(5,2),
    last_computed_at     DATETIME       NOT NULL,
    period_from          DATE,
    period_to            DATE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_heat_constituency ON constituency_heatmap_cache(constituency);
-- CREATE INDEX IF NOT EXISTS idx_heat_district     ON constituency_heatmap_cache(district);
-- CREATE INDEX IF NOT EXISTS idx_heat_scheme       ON constituency_heatmap_cache(scheme_type);

-- NPP BLOCK/DISTRICT INTERACTION LOG
CREATE TABLE IF NOT EXISTS npp_interaction_log (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    schedule_event_id     BIGINT       NOT NULL,
    level                 VARCHAR(20)  NOT NULL COMMENT 'BLOCK, DISTRICT',
    location_name         VARCHAR(200),
    duration_minutes      INT,
    decided_by            VARCHAR(50),
    decision              VARCHAR(20)  COMMENT 'YES, NO',
    npp_interaction_id    BIGINT,
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_npp_event FOREIGN KEY (schedule_event_id) REFERENCES schedule_events(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- APPOINTMENT REJECTION HISTORY
CREATE TABLE IF NOT EXISTS appointment_rejection_history (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    appointment_id   BIGINT       NOT NULL,
    rejected_by_role VARCHAR(50)  NOT NULL,
    rejected_by_user VARCHAR(100) NOT NULL,
    rejection_type   VARCHAR(30)  NOT NULL COMMENT 'REJECTED, SNOOZED, SENT_BACK',
    snooze_days      INT,
    remarks          TEXT,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rej_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX IF NOT EXISTS idx_rej_appt ON appointment_rejection_history(appointment_id);
