-- Run once with an infrastructure/database-administrator account.
-- The application account is not expected to have CREATE DATABASE privilege.
CREATE DATABASE IF NOT EXISTS meghaconnect_legacy
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Create/grant a least-privilege account according to the deployment secret policy.
-- Example only (do not commit a password):
-- CREATE USER 'meghaconnect_legacy_app'@'%' IDENTIFIED BY '<secret>';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
--   ON meghaconnect_legacy.* TO 'meghaconnect_legacy_app'@'%';
