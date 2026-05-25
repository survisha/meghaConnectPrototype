# MeghaConnect Security Fixes Summary

Date: 25 May 2026

## Mobile Fixes

- Replaced placeholder Android package IDs with `in.gov.meghalaya.*` IDs.
- Added production release signing configuration using `key.properties` or CI secrets.
- Added release build gates for HTTPS API URLs and no demo/mock flags.
- Disabled Android cleartext traffic.
- Removed broad legacy storage permission from the main app.
- Moved main app JWT/user session storage to `flutter_secure_storage`.
- Added Privacy Policy and Terms links to login.
- Added consent checkbox/version/timestamp capture for identity/photo/document/appointment flows.
- Gated demo login buttons behind non-production dart defines.
- Removed hardcoded demo password generation utilities; added a migration to disable old seeded accounts if they still use known default hashes.

## Backend Fixes

- Removed OTP values from API responses.
- Removed fixed/mock KYC OTP value.
- Added rate-limit filter for OTP, KYC, AI, and QR API groups.
- Removed hardcoded OVSE API key default; `OVSE_API_KEY` must be configured externally.
- Prevented production EPIC mock fallback when prod profile is active and EPIC credentials are missing.
- Removed wildcard `@CrossOrigin(origins="*")` from controllers and centralized CORS.
- Restricted AI document, AI summary, AI dashboard, duplicate check, priority, and slot APIs to staff roles.
- Restricted legacy KYC validation and face validation to staff roles.
- Restricted deprecated public registration controller to admin role.
- Enforced visitor-owner or staff access for visitor profile and KYC retry.
- Enforced authenticated visitor/applicant match for citizen appointment submission.
- Restricted appointment list/detail endpoints to staff roles.
- Added malware scanning service hook for file uploads and live-photo storage.
- Persisted consent metadata for visitors and appointments.
- Updated QR scanner tests to current repository signature and verified QR invalid/replay-related cases.

## Manual Security Confirmations

- Production keystore and Play App Signing custody.
- Production API host, CORS origins, TLS certificates, and DB TLS.
- SMS/WhatsApp live gateway and OTP template approval.
- EPIC and OVSE production credentials and callback allowlisting.
- Antivirus/malware scanning engine integration behind `MalwareScanService`.
- Certificate pinning, screenshot prevention, and root/jailbreak policy.
- Store reviewer credentials and sample QR/pass.
- iOS scope and Apple signing.

## Verification

- `flutter analyze` and `flutter test` passed for main app.
- `flutter analyze` and `flutter test` passed for scanner app.
- `mvn compile` and `mvn test` passed for backend.
