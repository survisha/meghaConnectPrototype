-- ============================================================
-- V6: Visitor OTP temporary table for mobile-based login
-- ============================================================

CREATE TABLE IF NOT EXISTS visitor_otp_temp (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phone_number  VARCHAR(20)   NOT NULL,
    otp_code      VARCHAR(10)   NOT NULL,
    expires_at    DATETIME      NOT NULL,
    consumed      TINYINT(1)    NOT NULL DEFAULT 0,
    attempt_count INT           NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_otp_phone  ON visitor_otp_temp(phone_number);
CREATE INDEX idx_otp_expiry ON visitor_otp_temp(expires_at);

-- Add email column to persons table (used in visitor registration)
ALTER TABLE persons ADD COLUMN IF NOT EXISTS email VARCHAR(150) NULL AFTER phone_number;

-- Allow visitor auth endpoints (update security permit-list in SecurityConfig)
-- NOTE: /api/v1/visitor/auth/** is added to SecurityConfig.java permitAll() list
