-- Task 1: persisted account security state and department onboarding workflow.

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN last_failed_login_at DATETIME NULL,
    ADD COLUMN locked_at DATETIME NULL,
    ADD COLUMN lock_reason VARCHAR(200) NULL,
    ADD COLUMN password_changed_at DATETIME NULL,
    ADD COLUMN temporary_password_created_at DATETIME NULL,
    ADD COLUMN credentials_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN unlocked_by VARCHAR(100) NULL,
    ADD COLUMN unlocked_at DATETIME NULL;

CREATE INDEX idx_users_department_role ON users(department_id, role);
CREATE INDEX idx_users_lock_status ON users(locked, failed_login_attempts);

INSERT INTO roles (role_name, description, created_at)
VALUES
    ('DEO', 'Department data entry operator', NOW()),
    ('HEAD_DEPARTMENT', 'Head of department', NOW())
ON DUPLICATE KEY UPDATE description = VALUES(description), updated_at = NOW();

CREATE TABLE department_access_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NULL,
    department_name VARCHAR(200) NOT NULL,
    department_code VARCHAR(50) NOT NULL,
    nodal_officer_name VARCHAR(150) NOT NULL,
    official_email VARCHAR(150) NOT NULL,
    official_mobile VARCHAR(20) NOT NULL,
    request_purpose VARCHAR(500) NOT NULL,
    expected_user_count INT NOT NULL,
    remarks VARCHAR(1000) NULL,
    supporting_document_path VARCHAR(500) NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    reviewed_by VARCHAR(100) NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_department_request_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT chk_department_request_user_count CHECK (expected_user_count > 0),
    KEY idx_department_request_status (request_status, submitted_at),
    KEY idx_department_request_code (department_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
