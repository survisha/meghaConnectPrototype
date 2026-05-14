-- ============================================================
-- MeghaConnect V38: Normalize appointment status column
-- MySQL 8 Compatible
-- ============================================================
--
-- Some environments were created from database/schema.sql, where
-- appointments.status was a MySQL ENUM. The backend now owns status
-- values through Appointment.AppointmentStatus and includes workflow
-- states such as CREATED, PENDING_APPROVER_REVIEW, FOLLOWUP, and
-- SELECTED_FOR_PUBLIC_DARBAR. Keep the database flexible by storing
-- the enum name as a VARCHAR.

UPDATE appointments
SET status = 'SUBMITTED'
WHERE status IS NULL OR TRIM(status) = '';

ALTER TABLE appointments
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED';
