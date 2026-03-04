-- ============================================================
-- V5: Grievances table + public demo user
-- ============================================================

CREATE TABLE IF NOT EXISTS grievances (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id           VARCHAR(30)   NOT NULL UNIQUE,
    applicant_name      VARCHAR(200)  NOT NULL,
    phone_number        VARCHAR(20),
    district            VARCHAR(100),
    constituency        VARCHAR(100),
    category            VARCHAR(50)   NOT NULL,
    subject             VARCHAR(300)  NOT NULL,
    description         TEXT          NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'SUBMITTED',
    submitted_at        DATETIME      NOT NULL,
    resolved_at         DATETIME,
    assigned_department VARCHAR(200),
    remarks             TEXT,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CREATE INDEX idx_grievance_ticket ON grievances(ticket_id);
-- CREATE INDEX idx_grievance_status ON grievances(status);
-- CREATE INDEX idx_grievance_phone  ON grievances(phone_number);

-- Demo public user (password: public123)
INSERT IGNORE INTO users (username, password_hash, full_name, role, active, offline_access, created_at)
VALUES ('public1', '$2a$12$Tn/kqOPqxIY5oHGJfB2.xOnpQwVYLV4A2usmqyQ.1Y2lHPL4C1NaK', 'Public Visitor', 'PUBLIC', 1, 0, NOW());
