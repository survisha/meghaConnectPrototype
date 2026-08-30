CREATE TABLE citizen_consents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    visitor_id BIGINT NOT NULL,
    consent_purposes VARCHAR(100) NOT NULL,
    consent_version VARCHAR(50) NOT NULL,
    consent_text VARCHAR(1000) NOT NULL,
    consent_granted BIT(1) NOT NULL,
    consented_at DATETIME NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recorded_by VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_citizen_consents_visitor FOREIGN KEY (visitor_id) REFERENCES visitors(id),
    INDEX idx_citizen_consents_visitor (visitor_id)
);
