ALTER TABLE document_uploads
    ADD COLUMN uploader_role VARCHAR(50) NULL AFTER uploaded_by,
    ADD COLUMN remarks VARCHAR(1000) NULL AFTER uploader_role;
