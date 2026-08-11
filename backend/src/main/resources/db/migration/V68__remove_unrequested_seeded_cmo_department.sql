-- Remove the default CMO tenant introduced by V60 unless CMO has subsequently
-- been established through an approved department access request.
-- The CMO reference_data option remains available for submitting that request.

SET @seeded_cmo_department_id = (
    SELECT id
    FROM departments
    WHERE UPPER(TRIM(department_code)) = 'CMO'
      AND LOWER(TRIM(COALESCE(created_by, ''))) = 'flyway'
    LIMIT 1
);

SET @has_approved_cmo_request = (
    SELECT EXISTS (
        SELECT 1
        FROM department_access_requests
        WHERE department_id = @seeded_cmo_department_id
          AND request_status = 'APPROVED'
    )
);

UPDATE users
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE appointments
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE visitors
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE schedule_events
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE audit_logs
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE appointment_qr_token
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE qr_scan_audit_log
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

UPDATE visitor_movement_log
SET department_id = NULL
WHERE department_id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;

DELETE FROM departments
WHERE id = @seeded_cmo_department_id
  AND @has_approved_cmo_request = 0;
