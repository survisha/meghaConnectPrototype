ALTER TABLE visitors
    ADD COLUMN consent_accepted BIT(1) NULL,
    ADD COLUMN consent_version VARCHAR(50) NULL,
    ADD COLUMN consent_timestamp DATETIME NULL,
    ADD COLUMN privacy_policy_url VARCHAR(500) NULL,
    ADD COLUMN terms_url VARCHAR(500) NULL;
