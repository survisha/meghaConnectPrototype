ALTER TABLE visitors
    ADD COLUMN face_enrollment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN face_enrolled_on DATETIME NULL,
    ADD COLUMN face_enrollment_message VARCHAR(500) NULL;

CREATE INDEX idx_visitor_face_enrollment_status ON visitors (face_enrollment_status);
