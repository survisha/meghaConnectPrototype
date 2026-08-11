ALTER TABLE document_uploads ADD COLUMN follow_up_id BIGINT NULL;
CREATE INDEX idx_document_follow_up ON document_uploads (follow_up_id);
ALTER TABLE document_uploads
    ADD CONSTRAINT fk_document_follow_up FOREIGN KEY (follow_up_id) REFERENCES direction_follow_ups(id);
