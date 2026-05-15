-- MeghaConnect V40: AI notes generated from uploaded appointment documents.
CREATE TABLE IF NOT EXISTS appointment_document_ai_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    file_name VARCHAR(300) NULL,
    ai_summary TEXT NULL,
    important_details TEXT NULL,
    missing_info TEXT NULL,
    risk_flags TEXT NULL,
    raw_ai_response MEDIUMTEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message TEXT NULL,
    model_name VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_notes_document (document_id),
    KEY idx_ai_notes_appointment (appointment_id),
    KEY idx_ai_notes_status (status),
    CONSTRAINT fk_ai_notes_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_notes_document
        FOREIGN KEY (document_id) REFERENCES document_uploads(id)
        ON DELETE CASCADE
);
