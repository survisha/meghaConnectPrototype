ALTER TABLE visitors
    ADD COLUMN mobile_otp_verification VARCHAR(20) NULL;

-- Existing registrations do not retain server-side OTP proof, so verification
-- cannot be established safely for historical rows.
UPDATE visitors
SET mobile_otp_verification = 'NOT_VERIFIED'
WHERE mobile_otp_verification IS NULL;

ALTER TABLE visitors
    MODIFY COLUMN mobile_otp_verification VARCHAR(20) NOT NULL DEFAULT 'NOT_VERIFIED';

ALTER TABLE visitor_otp_temp
    ADD COLUMN verification_token VARCHAR(64) NULL,
    ADD COLUMN verified_at DATETIME NULL,
    ADD COLUMN registration_consumed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uk_visitor_otp_verification_token
    ON visitor_otp_temp (verification_token);
