ALTER TABLE legacy_person_index
    ADD COLUMN normalized_epic VARCHAR(50) NULL AFTER epic,
    ADD COLUMN normalized_mobile VARCHAR(20) NULL AFTER mobile,
    ADD COLUMN village VARCHAR(180) NULL AFTER normalized_mobile,
    ADD COLUMN normalized_village VARCHAR(180) NULL AFTER village,
    ADD COLUMN address VARCHAR(500) NULL AFTER normalized_village,
    ADD COLUMN normalized_address VARCHAR(500) NULL AFTER address;

UPDATE legacy_person_index SET normalized_epic = UPPER(REPLACE(TRIM(epic), ' ', '')) WHERE epic IS NOT NULL;
UPDATE legacy_person_index SET normalized_mobile = RIGHT(REGEXP_REPLACE(mobile, '[^0-9]', ''), 10) WHERE mobile IS NOT NULL;

CREATE INDEX idx_legacy_person_normalized_epic ON legacy_person_index(normalized_epic);
CREATE INDEX idx_legacy_person_normalized_mobile ON legacy_person_index(normalized_mobile);
CREATE INDEX idx_legacy_person_district ON legacy_person_index(district);
CREATE INDEX idx_legacy_person_constituency ON legacy_person_index(constituency);
CREATE INDEX idx_legacy_person_name_village ON legacy_person_index(normalized_name, normalized_village);
CREATE INDEX idx_legacy_person_name_district ON legacy_person_index(normalized_name, district);
