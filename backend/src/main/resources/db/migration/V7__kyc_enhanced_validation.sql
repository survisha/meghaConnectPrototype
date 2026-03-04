-- ============================================================
-- MeghaConnect V7: KYC Enhanced Validation Fields – MySQL 8
-- ============================================================
-- Adds support for multi-step KYC validation with:
--   - Photo from ID card (base64 encoded)
--   - Live captured photo (base64 encoded)
--   - KYC status tracking (PHOTO_MATCHED, DEMOGRAPHIC_MATCHED, FAILED)
-- ============================================================

-- 1. ADD KYC PHOTO AND STATUS FIELDS TO PERSONS TABLE (idempotent for MySQL < 8.0.29)
SET @dbname = DATABASE();
SET @tablename = 'persons';
SET @columnname = 'photo_from_id_base64';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' LONGTEXT COMMENT ''Base64-encoded photo extracted from ID card (EPIC/Aadhaar)''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'live_photo_base64';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' LONGTEXT COMMENT ''Base64-encoded live photo captured during registration''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'kyc_status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(50) COMMENT ''PENDING | PHOTO_MATCHED | DEMOGRAPHIC_MATCHED | FAILED | NOT_VERIFIED''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. CREATE INDEX FOR KYC STATUS QUERIES
-- CREATE INDEX idx_person_kyc_status ON persons(kyc_status);

-- 3. ADD COMMENT TO EXISTING kycVerified COLUMN
-- This column remains as boolean flag; kyc_status provides granular status

-- 4. UPDATE EXISTING RECORDS WITH DEFAULT KYC STATUS
UPDATE persons 
SET kyc_status = CASE 
    WHEN kyc_verified = 1 THEN 'DEMOGRAPHIC_MATCHED'
    ELSE 'PENDING'
END
WHERE kyc_status IS NULL;

-- ============================================================
-- NOTES:
-- ============================================================
-- • photo_from_id_base64: Stores the photo extracted from the ID card
--   during the KYC validation process. In production, this would come
--   from the EPIC/Aadhaar API response.
--
-- • live_photo_base64: Stores the live photo captured via camera during
--   registration. Used for face matching against photo_from_id_base64.
--
-- • kyc_status: Tracks the granular KYC verification status:
--   - PENDING: Default state, KYC not yet initiated
--   - PHOTO_MATCHED: Live photo matched with ID card photo via face recognition
--   - DEMOGRAPHIC_MATCHED: OTP verified and demographics matched, but no photo match
--   - FAILED: KYC validation failed (photo mismatch or demographic mismatch)
--   - NOT_VERIFIED: User registered without completing KYC
--
-- • LONGTEXT column type allows storing large base64-encoded images
--   (typical size: 100KB - 500KB per image). In production, consider
--   migrating to object storage (S3/Azure Blob) and storing only URLs.
--
-- • This is a MOCK/DEMO implementation. Production should:
--   1. Store images in S3/Azure Blob Storage (not in database)
--   2. Store only object keys/URLs in database
--   3. Implement proper face recognition service integration
--   4. Add audit trail for all KYC validation attempts
-- ============================================================
