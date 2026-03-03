-- ============================================================
-- MeghaConnect V4: Public Registration + KYC – MySQL 8
-- ============================================================

-- 1. ADD AADHAAR & KYC FIELDS TO PERSONS
ALTER TABLE persons
    ADD COLUMN aadhaar_number      VARCHAR(20)  COMMENT 'Aadhaar (12-digit). KYC fallback when EPIC unavailable.',
    ADD COLUMN kyc_type            VARCHAR(10)  COMMENT 'EPIC | AADHAR | NONE',
    ADD COLUMN kyc_verified        TINYINT(1)   DEFAULT 0,
    ADD COLUMN kyc_verified_at     DATETIME,
    ADD COLUMN photo_storage_path  VARCHAR(500) COMMENT 'Path/key in file store, e.g. persons/42/photo.jpg';

CREATE INDEX idx_person_aadhaar ON persons(aadhaar_number);
CREATE INDEX idx_person_kyc     ON persons(kyc_type, kyc_verified);

-- 2. PUBLIC REGISTRATIONS
CREATE TABLE public_registrations (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    registration_token      VARCHAR(64)  NOT NULL UNIQUE,
    full_name               VARCHAR(200) NOT NULL,
    phone_number            VARCHAR(20)  NOT NULL,
    kyc_type                VARCHAR(10)  NOT NULL DEFAULT 'NONE' COMMENT 'EPIC | AADHAR | NONE',
    epic_number             VARCHAR(50),
    aadhaar_number          VARCHAR(20),
    photo_storage_path      VARCHAR(500),
    epic_scan_path          VARCHAR(500),
    aadhaar_scan_path       VARCHAR(500),
    date_of_birth           DATE,
    designation             VARCHAR(100),
    district                VARCHAR(100),
    constituency            VARCHAR(100),
    booth                   VARCHAR(100),
    village                 VARCHAR(100),
    address                 VARCHAR(500),
    kyc_verified            TINYINT(1)   NOT NULL DEFAULT 0,
    kyc_verified_at         DATETIME,
    kyc_verification_source VARCHAR(50)  COMMENT 'ELECTION_COMMISSION_API | UIDAI_API | MANUAL',
    matched_person_id       BIGINT,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | OTP_VERIFIED | KYC_DONE | MATCHED | COMPLETED | REJECTED',
    rejection_reason        TEXT,
    otp_verified_at         DATETIME,
    completed_at            DATETIME,
    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_pubreg_person FOREIGN KEY (matched_person_id) REFERENCES persons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pub_reg_phone   ON public_registrations(phone_number);
CREATE INDEX idx_pub_reg_status  ON public_registrations(status);
CREATE INDEX idx_pub_reg_epic    ON public_registrations(epic_number);
CREATE INDEX idx_pub_reg_aadhaar ON public_registrations(aadhaar_number);
CREATE INDEX idx_pub_reg_person  ON public_registrations(matched_person_id);

-- 3. KYC VERIFICATION LOG
CREATE TABLE kyc_verification_log (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    entity_type           VARCHAR(30)    NOT NULL COMMENT 'PUBLIC_REGISTRATION | PERSON',
    entity_id             BIGINT         NOT NULL,
    kyc_type              VARCHAR(10)    NOT NULL COMMENT 'EPIC | AADHAR',
    id_value              VARCHAR(50)    NOT NULL,
    provider              VARCHAR(50)    NOT NULL COMMENT 'ELECTION_COMMISSION_API | UIDAI_API | MOCK',
    api_endpoint          VARCHAR(300),
    success               TINYINT(1)     NOT NULL DEFAULT 0,
    response_code         VARCHAR(20),
    response_message      TEXT,
    verified_name         VARCHAR(200),
    verified_dob          DATE,
    name_match_score      DECIMAL(5,2),
    requested_at          DATETIME       NOT NULL,
    responded_at          DATETIME,
    request_id            VARCHAR(100),
    created_by            VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_kyc_entity   ON kyc_verification_log(entity_type, entity_id);
CREATE INDEX idx_kyc_type_val ON kyc_verification_log(kyc_type, id_value);
CREATE INDEX idx_kyc_provider ON kyc_verification_log(provider);
