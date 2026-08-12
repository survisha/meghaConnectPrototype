ALTER TABLE appointments ADD COLUMN rejected_at DATETIME NULL AFTER rejection_reason;

UPDATE appointments a
LEFT JOIN (
    SELECT appointment_id, MIN(created_at) AS rejected_at
    FROM appointment_audit
    WHERE new_status = 'REJECTED'
    GROUP BY appointment_id
) history ON history.appointment_id = a.id
SET a.rejected_at = COALESCE(history.rejected_at, a.updated_at, a.created_at)
WHERE a.status = 'REJECTED' AND a.rejected_at IS NULL;

CREATE INDEX idx_appt_status_completed_at ON appointments (status, completed_at);
CREATE INDEX idx_appt_status_rejected_at ON appointments (status, rejected_at);
CREATE INDEX idx_appt_status_department ON appointments (status, department_id);
CREATE INDEX idx_appt_applicant_status ON appointments (applicant_id, status);
CREATE INDEX idx_appt_agenda_status ON appointments (agenda_type, status);
CREATE INDEX idx_scheme_appointment_type ON scheme_applications (appointment_id, scheme_type);
CREATE INDEX idx_appt_audit_appointment_created ON appointment_audit (appointment_id, created_at);
CREATE INDEX idx_document_appointment_uploaded ON document_uploads (appointment_id, uploaded_date);
