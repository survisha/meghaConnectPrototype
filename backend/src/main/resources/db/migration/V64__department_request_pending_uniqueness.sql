-- Enforce one unresolved request per department under concurrent submissions.
ALTER TABLE department_access_requests
    ADD COLUMN pending_department_code VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE WHEN request_status = 'PENDING' THEN UPPER(department_code) ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY uk_department_request_pending_code (pending_department_code),
    ADD INDEX idx_department_request_submitted_at (submitted_at),
    ADD INDEX idx_department_request_official_email (official_email);
