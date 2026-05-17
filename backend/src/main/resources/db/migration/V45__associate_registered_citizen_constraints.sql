-- Associate visitors now reference registered citizens through associate_mappings.person_id.
-- MySQL permits multiple NULL values in a unique index, so legacy text-only rows remain readable
-- while new registered-citizen rows are protected from duplicates.

CREATE INDEX idx_assoc_person_id ON associate_mappings (person_id);

CREATE UNIQUE INDEX uk_assoc_appointment_person
    ON associate_mappings (appointment_id, person_id);
