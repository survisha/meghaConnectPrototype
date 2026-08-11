-- Support department-scoped, newest-first audit trail queries.

CREATE INDEX idx_audit_department_created_at
    ON audit_logs (department_id, created_at);

ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_logs_department
    FOREIGN KEY (department_id) REFERENCES departments(id);
