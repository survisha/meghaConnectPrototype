CREATE TABLE IF NOT EXISTS ai_call_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(128),
    module_name VARCHAR(80),
    user_id VARCHAR(100),
    prompt_type VARCHAR(80),
    provider VARCHAR(40),
    model VARCHAR(120),
    request_time DATETIME NOT NULL,
    duration_ms BIGINT,
    success BIT(1) NOT NULL DEFAULT b'0',
    error_message VARCHAR(500),
    INDEX idx_ai_call_audit_request_time (request_time),
    INDEX idx_ai_call_audit_module (module_name),
    INDEX idx_ai_call_audit_provider (provider)
);
