-- Quick fix: Update demo user passwords with correct BCrypt hashes
-- AND remove old V10 migration from Flyway history so it re-runs
USE meghaconnect_db;

-- Remove the old (bad) V10 migration from Flyway history
DELETE FROM flyway_schema_history WHERE version = '10';

-- Update passwords immediately (in case backend doesn't restart)
UPDATE users SET password_hash = '$2a$10$Se9DjTVUDtzAHo2W/mf0JuG1bSMGHGhU8cvLnoUyW2PQqUc89oOa.' WHERE username = 'hcm';      -- hcm123
UPDATE users SET password_hash = '$2a$10$.23hPl3rkhCSFxCZ68G4I.ik80KwbH/KBGwACiSCofQbRgBp4S55i' WHERE username = 'admin';    -- admin123
UPDATE users SET password_hash = '$2a$10$Zwhd4TUrkWsPX012FeMmZulCIyMp4pIt7KTw/5qdif6bnJeUab3l.' WHERE username = 'saidul';   -- osd123
UPDATE users SET password_hash = '$2a$10$p0uXTJCS/2xKkuzV7kQ0fOL9sXBMSe.mPj86a0e7SbbovcW.41QF2' WHERE username = 'jtsecy';   -- jts123
UPDATE users SET password_hash = '$2a$10$ZE0TyDLqBeVnqKeSSH0JjOyGT0BCqPmSu4wM0m5GxTt3YUyFt64ua' WHERE username = 'cmo';      -- cmo123
UPDATE users SET password_hash = '$2a$10$HkySIP9NjInkhzLE82XnTeVhMlTtz7b/LFypv8zVI6mkKHD0D9jK2' WHERE username = 'deo1';     -- deo123

SELECT '=== Password hashes updated ===' as status;
SELECT username, LEFT(password_hash, 30) as hash_preview, role, active FROM users WHERE username IN ('hcm', 'admin', 'saidul', 'jtsecy', 'cmo', 'deo1');
