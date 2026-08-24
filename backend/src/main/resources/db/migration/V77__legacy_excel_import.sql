CREATE TABLE legacy_dataset_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_code VARCHAR(80) NOT NULL,
    dataset_name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    target_table VARCHAR(80) NOT NULL DEFAULT 'legacy_dataset_record',
    category VARCHAR(80),
    duplicate_key_fields VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(100),
    updated_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_dataset_code (dataset_code),
    CONSTRAINT ck_legacy_target_table CHECK (target_table = 'legacy_dataset_record')
);

CREATE TABLE legacy_dataset_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_definition_id BIGINT NOT NULL,
    target_field_name VARCHAR(80) NOT NULL,
    target_data_type VARCHAR(20) NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    identifier_type VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_dataset_field (dataset_definition_id, target_field_name),
    CONSTRAINT fk_legacy_column_dataset FOREIGN KEY (dataset_definition_id)
        REFERENCES legacy_dataset_definition(id)
);

CREATE TABLE legacy_dataset_column_alias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_column_id BIGINT NOT NULL,
    source_column_alias VARCHAR(160) NOT NULL,
    normalized_alias VARCHAR(160) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_column_alias (dataset_column_id, normalized_alias),
    CONSTRAINT fk_legacy_alias_column FOREIGN KEY (dataset_column_id)
        REFERENCES legacy_dataset_column(id)
);

CREATE TABLE legacy_import_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at DATETIME NOT NULL,
    total_sheets INT NOT NULL DEFAULT 0,
    analyzed_sheets INT NOT NULL DEFAULT 0,
    imported_sheets INT NOT NULL DEFAULT 0,
    failed_sheets INT NOT NULL DEFAULT 0,
    skipped_sheets INT NOT NULL DEFAULT 0,
    mapping_required_sheets INT NOT NULL DEFAULT 0,
    total_rows BIGINT NOT NULL DEFAULT 0,
    valid_rows BIGINT NOT NULL DEFAULT 0,
    imported_rows BIGINT NOT NULL DEFAULT 0,
    failed_rows BIGINT NOT NULL DEFAULT 0,
    duplicate_rows BIGINT NOT NULL DEFAULT 0,
    overall_status VARCHAR(30) NOT NULL,
    started_at DATETIME,
    completed_at DATETIME,
    PRIMARY KEY (id),
    KEY idx_legacy_batch_user_time (uploaded_by, uploaded_at),
    KEY idx_legacy_batch_status (overall_status)
);

CREATE TABLE legacy_import_sheet (
    id BIGINT NOT NULL AUTO_INCREMENT,
    import_batch_id BIGINT NOT NULL,
    sheet_index INT NOT NULL,
    sheet_name VARCHAR(255) NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    detected_header_row INT,
    total_columns INT NOT NULL DEFAULT 0,
    total_rows BIGINT NOT NULL DEFAULT 0,
    detected_dataset_id BIGINT,
    confirmed_dataset_id BIGINT,
    target_table VARCHAR(80),
    mapping_confidence DECIMAL(5,2),
    valid_rows BIGINT NOT NULL DEFAULT 0,
    imported_rows BIGINT NOT NULL DEFAULT 0,
    failed_rows BIGINT NOT NULL DEFAULT 0,
    duplicate_rows BIGINT NOT NULL DEFAULT 0,
    skipped_rows BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    status_reason VARCHAR(255),
    started_at DATETIME,
    completed_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_batch_sheet_index (import_batch_id, sheet_index),
    CONSTRAINT fk_legacy_sheet_batch FOREIGN KEY (import_batch_id) REFERENCES legacy_import_batch(id),
    CONSTRAINT fk_legacy_sheet_detected_dataset FOREIGN KEY (detected_dataset_id) REFERENCES legacy_dataset_definition(id),
    CONSTRAINT fk_legacy_sheet_confirmed_dataset FOREIGN KEY (confirmed_dataset_id) REFERENCES legacy_dataset_definition(id)
);

CREATE TABLE legacy_import_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    import_sheet_id BIGINT NOT NULL,
    source_column_index INT NOT NULL,
    source_column_name VARCHAR(255) NOT NULL,
    normalized_column_name VARCHAR(160) NOT NULL,
    detected_data_type VARCHAR(20) NOT NULL,
    mapped_target_field VARCHAR(80),
    mapped_identifier_type VARCHAR(30),
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    ignored BOOLEAN NOT NULL DEFAULT FALSE,
    mapping_status VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_sheet_column_index (import_sheet_id, source_column_index),
    CONSTRAINT fk_legacy_import_column_sheet FOREIGN KEY (import_sheet_id) REFERENCES legacy_import_sheet(id)
);

CREATE TABLE legacy_import_error (
    id BIGINT NOT NULL AUTO_INCREMENT,
    import_batch_id BIGINT NOT NULL,
    import_sheet_id BIGINT NOT NULL,
    sheet_name VARCHAR(255) NOT NULL,
    source_row_number BIGINT NOT NULL,
    column_name VARCHAR(255),
    raw_value VARCHAR(500),
    error_code VARCHAR(60) NOT NULL,
    error_message VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_legacy_error_batch_sheet (import_batch_id, import_sheet_id),
    CONSTRAINT fk_legacy_error_batch FOREIGN KEY (import_batch_id) REFERENCES legacy_import_batch(id),
    CONSTRAINT fk_legacy_error_sheet FOREIGN KEY (import_sheet_id) REFERENCES legacy_import_sheet(id)
);

CREATE TABLE legacy_dataset_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_definition_id BIGINT NOT NULL,
    dataset_code VARCHAR(80) NOT NULL,
    record_fingerprint VARCHAR(64) NOT NULL,
    record_data JSON NOT NULL,
    source_file VARCHAR(255) NOT NULL,
    source_sheet VARCHAR(255) NOT NULL,
    source_row_number BIGINT NOT NULL,
    import_batch_id BIGINT NOT NULL,
    imported_by VARCHAR(100) NOT NULL,
    imported_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_source_trace (import_batch_id, source_sheet, source_row_number, dataset_definition_id),
    UNIQUE KEY uk_legacy_dataset_fingerprint (dataset_definition_id, record_fingerprint),
    CONSTRAINT fk_legacy_record_dataset FOREIGN KEY (dataset_definition_id) REFERENCES legacy_dataset_definition(id),
    CONSTRAINT fk_legacy_record_batch FOREIGN KEY (import_batch_id) REFERENCES legacy_import_batch(id)
);

CREATE TABLE legacy_person_index (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_dataset_code VARCHAR(80) NOT NULL,
    source_table VARCHAR(80) NOT NULL,
    source_record_id BIGINT NOT NULL,
    name VARCHAR(255),
    normalized_name VARCHAR(255),
    epic VARCHAR(50),
    mobile VARCHAR(20),
    district VARCHAR(120),
    constituency VARCHAR(120),
    scheme_code VARCHAR(80),
    identity_basis VARCHAR(20) NOT NULL,
    source_file VARCHAR(255) NOT NULL,
    source_sheet VARCHAR(255) NOT NULL,
    source_row_number BIGINT NOT NULL,
    import_batch_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_person_source (source_table, source_record_id),
    KEY idx_legacy_person_epic (epic),
    KEY idx_legacy_person_mobile (mobile),
    KEY idx_legacy_person_name (normalized_name),
    CONSTRAINT fk_legacy_person_record FOREIGN KEY (source_record_id) REFERENCES legacy_dataset_record(id),
    CONSTRAINT fk_legacy_person_batch FOREIGN KEY (import_batch_id) REFERENCES legacy_import_batch(id)
);
