-- V16__add_scheme_document_requirements.sql
-- Create table for configuring required documents per scheme
-- (Scheme master data is stored in reference_data table as CM_SCHEME type)

CREATE TABLE IF NOT EXISTS scheme_required_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_code VARCHAR(50) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_label VARCHAR(200) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    description LONGTEXT,
    file_format_allowed VARCHAR(100),
    display_order INT DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_scheme_doc_type (scheme_code, document_type),
    INDEX idx_scheme_doc_code (scheme_code),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample required documents for CMSDF scheme
INSERT IGNORE INTO scheme_required_documents (scheme_code, document_type, document_label, is_required, description, file_format_allowed, display_order, created_by, updated_by)
VALUES
    ('CMSDF', 'PLANS_ESTIMATES', 'Plans & Estimates (3 copies)', TRUE, 'Detailed project plans and cost estimates', 'pdf', 1, 'SYSTEM', 'SYSTEM'),
    ('CMSDF', 'BANK_DETAILS', 'Bank Account Details', TRUE, 'Valid bank account details for fund transfer', 'pdf,jpg', 2, 'SYSTEM', 'SYSTEM'),
    ('CMSDF', 'MLA_APPROVAL', 'MLA/MDC Approval Letter', FALSE, 'Letter of support from local MLA or MDC', 'pdf', 3, 'SYSTEM', 'SYSTEM');

-- Insert sample required documents for CM_CARE scheme
INSERT IGNORE INTO scheme_required_documents (scheme_code, document_type, document_label, is_required, description, file_format_allowed, display_order, created_by, updated_by)
VALUES
    ('CM_CARE', 'MEDICAL_DOCS', 'Medical Documents', TRUE, 'Hospital/clinic diagnosis and treatment documents', 'pdf,jpg', 1, 'SYSTEM', 'SYSTEM'),
    ('CM_CARE', 'FINANCIAL_PROOF', 'Financial Hardship Proof', TRUE, 'Income certificate or similar financial hardship documents', 'pdf,jpg', 2, 'SYSTEM', 'SYSTEM'),
    ('CM_CARE', 'BANK_DETAILS', 'Bank Account Details', TRUE, 'Valid bank account details for fund transfer', 'pdf,jpg', 3, 'SYSTEM', 'SYSTEM');

-- Insert sample required documents for CMSG scheme
INSERT IGNORE INTO scheme_required_documents (scheme_code, document_type, document_label, is_required, description, file_format_allowed, display_order, created_by, updated_by)
VALUES
    ('CMSG', 'PLANS_ESTIMATES', 'Plans & Estimates', TRUE, 'Project plans and cost estimates', 'pdf', 1, 'SYSTEM', 'SYSTEM'),
    ('CMSG', 'BANK_DETAILS', 'Bank Account Details', TRUE, 'Valid bank account details for fund transfer', 'pdf,jpg', 2, 'SYSTEM', 'SYSTEM');

