-- ============================================================
-- Fix Demo User Passwords - PROPER BCrypt Hashes
-- ============================================================
-- This migration fixes the broken BCrypt hashes from V10
-- All hashes are valid BCrypt (60 chars, proper format)
-- Generated with BCrypt strength 10 (Spring Security default)
-- 
-- DEMO USER CREDENTIALS:
-- ┌──────────┬─────────────┬─────────────────────────┐
-- │ Username │ Password    │ Role                    │
-- ├──────────┼─────────────┼─────────────────────────┤
-- │ hcm      │ hcm123      │ HCM                     │
-- │ admin    │ admin123    │ ADMIN                   │
-- │ saidul   │ osd123      │ SAIDUL_OSD              │
-- │ jtsecy   │ jts123      │ APPROVER_JT_SECY        │
-- │ cmo      │ cmo123      │ CMO_OFFICER             │
-- │ deo1     │ deo123      │ DATA_ENTRY_OPERATOR     │
-- └──────────┴─────────────┴─────────────────────────┘

-- Update all demo user passwords with REAL BCrypt hashes (strength 10)
-- Hashes generated using Spring Security BCryptPasswordEncoder
UPDATE users SET password_hash = '$2a$10$Se9DjTVUDtzAHo2W/mf0JuG1bSMGHGhU8cvLnoUyW2PQqUc89oOa.' WHERE username = 'hcm';      -- hcm123
UPDATE users SET password_hash = '$2a$10$.23hPl3rkhCSFxCZ68G4I.ik80KwbH/KBGwACiSCofQbRgBp4S55i' WHERE username = 'admin';    -- admin123
UPDATE users SET password_hash = '$2a$10$Zwhd4TUrkWsPX012FeMmZulCIyMp4pIt7KTw/5qdif6bnJeUab3l.' WHERE username = 'saidul';   -- osd123
UPDATE users SET password_hash = '$2a$10$p0uXTJCS/2xKkuzV7kQ0fOL9sXBMSe.mPj86a0e7SbbovcW.41QF2' WHERE username = 'jtsecy';   -- jts123
UPDATE users SET password_hash = '$2a$10$ZE0TyDLqBeVnqKeSSH0JjOyGT0BCqPmSu4wM0m5GxTt3YUyFt64ua' WHERE username = 'cmo';      -- cmo123
UPDATE users SET password_hash = '$2a$10$HkySIP9NjInkhzLE82XnTeVhMlTtz7b/LFypv8zVI6mkKHD0D9jK2' WHERE username = 'deo1';     -- deo123


