-- Drop legacy tables that are no longer mapped by JPA entities and have no
-- runtime Java/Angular references. Historical migrations are left intact so
-- fresh databases can replay the original evolution before this cleanup.

DROP TABLE IF EXISTS visitor_associates;
DROP TABLE IF EXISTS public_registrations;
DROP TABLE IF EXISTS kyc_verification_log;
DROP TABLE IF EXISTS bank_account_details;
DROP TABLE IF EXISTS appointment_day_limits;
DROP TABLE IF EXISTS prior_scheme_history;
DROP TABLE IF EXISTS notification_log;
DROP TABLE IF EXISTS meeting_timer_log;
DROP TABLE IF EXISTS face_recognition_sources;
DROP TABLE IF EXISTS external_scheme_records;
DROP TABLE IF EXISTS approval_delegation_log;
DROP TABLE IF EXISTS constituency_heatmap_cache;
DROP TABLE IF EXISTS npp_interaction_log;
DROP TABLE IF EXISTS appointment_rejection_history;
