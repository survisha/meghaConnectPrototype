# Aadhaar Registration Removal Strategy

Phase 6 removes Aadhaar registration from application APIs and user interfaces. Existing Aadhaar-related columns are intentionally retained for now because production/UAT data must be audited before destructive schema changes.

Before dropping or nulling Aadhaar columns:

1. Run a UAT and production data inventory for `visitors.aadhaar_number`, `visitors.aadhaar_client_txn_id`, `visitors.aadhaar_app_id`, and any document paths that historically stored Aadhaar scans.
2. Confirm legal/data-retention requirements for legacy visitor records and audit trails.
3. Export or redact legacy Aadhaar data according to the approved retention decision.
4. Add a dedicated Flyway migration to drop or null the approved columns only after the audit is signed off.

Current code changes make the Aadhaar registration and Aadhaar KYC endpoints unavailable while preserving legacy records for safe review.
