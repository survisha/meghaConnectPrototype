-- ============================================================
-- MeghaConnect V4: Public Registration + KYC
-- ============================================================
-- Adds Aadhaar support to persons, introduces the public
-- self-registration flow with KYC verification tracking, and
-- clarifies the file-storage approach for photos & documents.
--
-- FILE STORAGE CONVENTION
-- Photos and scanned documents are NOT stored as BLOBs in the DB.
-- They are written to an external file store (local NFS, MinIO/S3,
-- or any compatible object store).  The table columns that end in
-- _path / _storage_path hold the resolved path or object key, e.g.:
--   • persons.photo_storage_path  → "persons/{id}/photo.jpg"
--   • document_uploads.file_path  → "documents/{type}/{uuid}.pdf"
-- The base URL of the file store is configured in application.yml
-- (meghaconnect.storage.base-url) so the backend assembles the full
-- URL without hard-coding it in the database.
-- ============================================================

-- 1. ADD AADHAAR FIELD TO PERSONS
--    EPIC is the primary KYC document; Aadhaar is accepted when the
--    person does not have an EPIC (e.g. minors, new citizens).
ALTER TABLE persons
    ADD COLUMN IF NOT EXISTS aadhaar_number       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS kyc_type            VARCHAR(10),  -- 'EPIC' | 'AADHAR' | 'NONE'
    ADD COLUMN IF NOT EXISTS kyc_verified        BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS kyc_verified_at     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS photo_storage_path  VARCHAR(500); -- replaces/extends the 200-char photo_path

COMMENT ON COLUMN persons.aadhaar_number IS
    'Aadhaar number (12-digit). Used as KYC fallback when EPIC is unavailable.';
COMMENT ON COLUMN persons.kyc_type IS
    'Which ID was used for KYC verification: EPIC, AADHAR, or NONE.';
COMMENT ON COLUMN persons.photo_storage_path IS
    'Path/key in the configured file store (e.g. persons/42/photo.jpg).';

CREATE INDEX IF NOT EXISTS idx_person_aadhaar ON persons(aadhaar_number);
CREATE INDEX IF NOT EXISTS idx_person_kyc    ON persons(kyc_type, kyc_verified);

-- 2. PUBLIC REGISTRATIONS
--    Captures a citizen's own self-registration before any appointment
--    is created.  The linked person_id is set once a DEO or the system
--    matches / creates the persons record.
CREATE TABLE public_registrations (
    id                      BIGSERIAL PRIMARY KEY,
    registration_token      VARCHAR(64)  UNIQUE NOT NULL,  -- OTP-verified unique token
    full_name               VARCHAR(200) NOT NULL,
    phone_number            VARCHAR(20)  NOT NULL,
    -- KYC: EPIC first, Aadhaar as fallback
    kyc_type                VARCHAR(10)  NOT NULL DEFAULT 'NONE',  -- EPIC | AADHAR | NONE
    epic_number             VARCHAR(50),
    aadhaar_number           VARCHAR(20),
    -- File-store paths (set after upload)
    photo_storage_path      VARCHAR(500),  -- e.g. "registrations/{token}/photo.jpg"
    epic_scan_path          VARCHAR(500),  -- e.g. "registrations/{token}/epic_scan.pdf"
    aadhaar_scan_path        VARCHAR(500),  -- e.g. "registrations/{token}/aadhaar_scan.pdf"
    -- Basic demographics
    date_of_birth           DATE,
    designation             VARCHAR(100),
    district                VARCHAR(100),
    constituency            VARCHAR(100),
    booth                   VARCHAR(100),
    village                 VARCHAR(100),
    address                 VARCHAR(500),
    -- KYC verification result
    kyc_verified            BOOLEAN DEFAULT FALSE,
    kyc_verified_at         TIMESTAMP,
    kyc_verification_source VARCHAR(50),   -- ELECTION_COMMISSION_API | UIDAI_API | MANUAL
    -- Linking to persons table (set once matched)
    matched_person_id       BIGINT REFERENCES persons(id),
    -- Workflow state
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
                                                  -- PENDING | OTP_VERIFIED | KYC_DONE |
                                                  -- MATCHED | COMPLETED | REJECTED
    rejection_reason        TEXT,
    otp_verified_at         TIMESTAMP,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);
CREATE INDEX idx_pub_reg_phone  ON public_registrations(phone_number);
CREATE INDEX idx_pub_reg_status ON public_registrations(status);
CREATE INDEX idx_pub_reg_epic   ON public_registrations(epic_number);
CREATE INDEX idx_pub_reg_aadhaar ON public_registrations(aadhaar_number);
CREATE INDEX idx_pub_reg_person ON public_registrations(matched_person_id);

-- 3. KYC VERIFICATION LOG
--    Each call to an external KYC API (Election Commission API for EPIC,
--    UIDAI API for Aadhaar) is recorded here for audit and retry tracking.
--    The architecture is plug-and-play: new provider adapters can be added
--    without changing the log structure.
CREATE TABLE kyc_verification_log (
    id                    BIGSERIAL PRIMARY KEY,
    -- What was verified
    entity_type           VARCHAR(30) NOT NULL,   -- PUBLIC_REGISTRATION | PERSON
    entity_id             BIGINT      NOT NULL,
    kyc_type              VARCHAR(10) NOT NULL,   -- EPIC | AADHAR
    id_value              VARCHAR(50) NOT NULL,   -- the actual EPIC / Aadhaar number sent
    -- Which external API was called
    provider              VARCHAR(50) NOT NULL,   -- ELECTION_COMMISSION_API | UIDAI_API | MOCK
    api_endpoint          VARCHAR(300),           -- full URL called (for debugging)
    -- Result
    success               BOOLEAN     NOT NULL,
    response_code         VARCHAR(20),
    response_message      TEXT,
    verified_name         VARCHAR(200),           -- name returned by the API
    verified_dob          DATE,                   -- DOB returned by the API (Aadhaar)
    name_match_score      NUMERIC(5,2),           -- fuzzy match % against provided name
    -- Timing
    requested_at          TIMESTAMP   NOT NULL,
    responded_at          TIMESTAMP,
    -- Metadata
    request_id            VARCHAR(100),           -- correlation / trace ID from provider
    created_by            VARCHAR(100)
);
CREATE INDEX idx_kyc_entity   ON kyc_verification_log(entity_type, entity_id);
CREATE INDEX idx_kyc_type_val ON kyc_verification_log(kyc_type, id_value);
CREATE INDEX idx_kyc_provider ON kyc_verification_log(provider);
