# Tasks 2–4 change report

## Database

- Reused `reference_type` and `reference_data`; no district/mandal tables were created.
- Added nullable `reference_data.parent_id` with a self-referencing foreign key and index.
- Reused the existing unique constraints on `reference_type.code` and `reference_data(type_id, code)`.
- Added `MEGHALAYA_DISTRICT` and `MEGHALAYA_DISTRICT_MANDAL`, 12 districts, and their supplied child constituencies in Flyway V63.
- Seed inserts use `NOT EXISTS`, while Flyway versioning prevents an applied migration from running twice.

Rollback guidance: remove the seeded mandals, districts, and two reference types by type/code; then drop `fk_reference_data_parent`, `idx_reference_data_parent`, and `parent_id`. Do this only after confirming no later reference type uses the hierarchy.

## APIs and clients

- Extended the existing reference endpoint with optional `parentCode`; only active parents and children are returned in display order.
- Added cached dependent dropdowns in Angular and Flutter registration.
- Added resolved face identification: provider credentials remain backend-only and only `VISITOR_<id>` enrollment IDs can resolve internal visitors.
- Added DEO capture/search/retake and reuse of an unmatched capture in the existing reviewed OCR/EPIC/OTP registration flow.
- Added public single-face identification in Angular and Flutter.
- Added Angular multi-face-crop aggregation with maximum 6 inputs, concurrency 3, stable input order, partial no-match handling, and visitor-ID deduplication.

## Existing structures retained

- Existing visitor address text fields remain the persistence model; reference display values are submitted through them to avoid duplicate columns.
- Existing visitor form extraction, EPIC verification, OTP, camera, image validation, and role-protected face endpoints remain authoritative.
- No biometric templates or provider credentials are stored or returned.

## Cleanup recommendations

- A later production migration may normalize visitor address reference codes if reporting requires them; do not add parallel nullable address columns without a backfill plan.
- Existing Angular bundle-budget and CommonJS warnings should be handled as a separate frontend optimization task.
