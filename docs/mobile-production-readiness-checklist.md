# MeghaConnect Mobile Production Readiness Checklist

Audit/fix date: 25 May 2026

Scope reviewed and updated:
- Main Flutter app: `mobile/`
- Security scanner Flutter app: `meghaconnect_security_scanner/`
- Backend mobile-facing API security: `backend/`

## Final Recommendation

Status: **CONDITIONAL GO for Android production build after manual confirmations. NO-GO for Apple App Store in this release.**

The critical code blockers found in the previous audit have been fixed or gated. Android can proceed to release-candidate build only after the manual confirmations listed below are completed. iOS remains **not included in this release** because no `ios/` Flutter projects are present in the current repository.

## Final Package Names Used

| App | Display Name | Android namespace/applicationId |
|---|---|---|
| Main app | MeghaConnect | `in.gov.meghalaya.meghaconnect` |
| Scanner app | MeghaConnect Security | `in.gov.meghalaya.meghaconnect.securityscanner` |

Manual confirmation required: IAS/NITCON/Government of Meghalaya must approve final publishing ownership. If NITCON is the approved publisher, replace package IDs with `in.nitcon.meghaconnect` and `in.nitcon.meghaconnect.securityscanner` before store submission.

## Fixed Items

| Area | Status | Files / Notes |
|---|---|---|
| Placeholder Android package removed | Fixed | `mobile/android/app/build.gradle.kts`, `mobile/android/app/src/main/kotlin/in/gov/meghalaya/meghaconnect/MainActivity.kt` |
| Scanner package no longer uses `com.survisha.*` | Fixed | `meghaconnect_security_scanner/android/app/build.gradle`, new Java package under `in/gov/meghalaya/...` |
| Release signing no longer uses debug signing | Fixed | Both Android Gradle files now require `key.properties` or CI env vars for release builds. |
| Release build validation | Fixed | Main app fails release if `MEGHA_API_BASE_URL` is missing, non-HTTPS, or demo credentials are enabled. Scanner fails release unless `APP_ENV=prod`, HTTPS `API_BASE_URL`, and mock flags are disabled. |
| Cleartext traffic disabled | Fixed | Main and scanner manifests set `android:usesCleartextTraffic="false"`. |
| Broad storage permission removed | Fixed | Main manifest keeps Internet, Camera, and scoped `READ_MEDIA_IMAGES`; broad legacy storage permission was removed. |
| Secure token storage | Fixed | Main app JWT/user session moved to `flutter_secure_storage` in `mobile/lib/core/security/secure_app_storage.dart`. |
| Demo credentials removed from production code | Fixed | Demo login buttons are now dart-define gated and release validation blocks `ENABLE_DEMO_CREDENTIALS=true`. |
| Known seeded accounts neutralized | Fixed | `V54__disable_known_seed_credentials.sql` disables old seeded accounts if their password hashes still match known defaults. Historical Flyway files were not rewritten to avoid checksum churn. |
| Privacy/terms links in login | Fixed | Main login screen exposes Privacy Policy and Terms links via dart defines. |
| Consent capture | Fixed | Citizen registration, public appointment, and guest appointment flows capture consent version/timestamp and send to backend. Backend persists visitor/appointment consent fields. |
| OTP not returned in backend responses | Fixed | Visitor OTP responses no longer include OTP values. KYC OTP no longer uses fixed `123456`. |
| Public backend endpoint surface reduced | Fixed | Sensitive visitor profile, KYC retry, legacy KYC validation/face, AI document/summary/dashboard, QR, file, and appointment endpoints now require JWT/RBAC. |
| IDOR controls | Fixed | Visitor profile/KYC retry enforce owner-or-staff access. Appointment submit enforces authenticated visitor/applicant match for citizen submissions. |
| Rate limiting hook | Fixed | Added API rate-limit filter for OTP, KYC, AI, and QR paths. |
| OVSE hardcoded key removed | Fixed | `OVSE_API_KEY` must come from environment/secret config. No default secret remains. |
| Wildcard controller CORS removed | Fixed | Per-controller `@CrossOrigin(origins="*")` removed; central CORS config is used. |
| File upload malware scan hook | Fixed | Added `MalwareScanService` interface and default placeholder service. Production AV engine still requires integration. |
| QR/scanner RBAC and target SDK | Fixed | Scanner target SDK set to 35; QR APIs are restricted to `SECURITY`/`ADMIN`; mock QR/auth flags are blocked in release. |
| QR tamper/replay tests | Verified | Existing QR tests cover invalid token, unauthorized role, duplicate check-in, duplicate checkout, and expired QR behavior. Test mocks were updated to current repository signature. |

## Remaining Manual Confirmations

| Item | Status |
|---|---|
| Final publishing organization and package ownership | Manual confirmation required |
| Production release keystore creation and secure storage | Manual confirmation required |
| Play App Signing enrollment and upload key custody | Manual confirmation required |
| Official production API URL | Manual confirmation required |
| Official Privacy Policy URL and Terms URL | Manual confirmation required |
| SMS/WhatsApp template approval and live gateway credentials | Manual confirmation required |
| EPIC production API credentials and enablement | Manual confirmation required |
| OVSE production API credentials and callback allowlisting | Manual confirmation required |
| Production CORS origins | Manual confirmation required |
| DB TLS/network security topology | Manual confirmation required |
| Certificate pinning policy | Manual confirmation required |
| Root/jailbreak detection policy | Manual confirmation required |
| Screenshot prevention policy for sensitive screens | Manual confirmation required |
| Antivirus/malware scanning engine selection | Manual confirmation required |
| Store screenshots, metadata, and reviewer notes | Manual confirmation required |
| Active app-review credentials and sample QR/pass | Manual confirmation required |
| iOS inclusion, Apple Developer Team, bundle IDs, signing | Not included in this release; manual confirmation required |

## Data Collection Table

| Data Type | Purpose | Stored Where | Shared With | Retention | Security Control |
|---|---|---|---|---|---|
| Name | Citizen identity, appointment review | Backend DB | Authorized staff | Manual policy required | RBAC, consent capture |
| Mobile number | OTP, contact, appointment updates | Backend DB | SMS/WhatsApp provider | Manual policy required | OTP rate limit, masking in logs |
| EPIC/Aadhaar reference | KYC and duplicate checks | Backend DB | EPIC/OVSE provider where applicable | Manual policy required | Validation, masking recommended |
| Photo | KYC/entry/security verification | Backend file store/DB reference | Authorized staff, future face API | Manual policy required | File path encryption/hash, consent |
| Documents | Scheme/appointment evidence | Backend file store/DB metadata | Authorized staff, OCR/AI service | Manual policy required | Authenticated streaming, type/size validation, AV hook |
| Appointment details | Workflow processing | Backend DB | Authorized staff | Manual policy required | JWT/RBAC, IDOR checks |
| QR scan logs | Entry validation/audit | Backend DB | Security/admin staff | Manual policy required | SECURITY/ADMIN role checks |
| Device/session data | Login/session management | Secure storage on device | Backend via auth headers | Until logout/expiry | `flutter_secure_storage`, token expiry cleanup |
| Consent metadata | Privacy compliance evidence | Backend DB | Authorized auditors/staff | Manual policy required | Consent version/timestamp persisted |

## Build Commands

Main app Android App Bundle:

```powershell
cd mobile
flutter build appbundle --release `
  --dart-define=MEGHA_API_BASE_URL=https://<production-api-host>/api `
  --dart-define=MEGHA_PRIVACY_POLICY_URL=https://<official-privacy-url> `
  --dart-define=MEGHA_TERMS_URL=https://<official-terms-url> `
  --dart-define=MEGHA_CONSENT_VERSION=2026-05-25
```

Scanner Android App Bundle:

```powershell
cd meghaconnect_security_scanner
flutter build appbundle --release `
  --dart-define=APP_ENV=prod `
  --dart-define=API_BASE_URL=https://<production-api-host> `
  --dart-define=USE_MOCK_AUTH=false `
  --dart-define=USE_MOCK_QR=false
```

Release signing must be supplied through `android/key.properties` or CI environment variables. Do not commit keystores or passwords.

## Store Submission Checklist

| Checklist Item | Status |
|---|---|
| Android `.aab` builds with final package ID | Ready after keystore and production API defines |
| Release signing configured outside Git | Ready after manual keystore setup |
| App icons/splash manually approved | Manual confirmation required |
| Permissions reviewed | Fixed in manifest; store declarations still manual |
| Privacy Policy and Terms links | Code-ready; official URLs required |
| Play Data Safety form | Manual submission required |
| App Store Privacy labels | Not included until iOS project exists |
| Reviewer credentials/sample data | Manual confirmation required |
| Screenshots/metadata | Manual confirmation required |
| Backend production secrets | Manual confirmation required |
| Production monitoring/audit review | Manual confirmation required |

## Verification Completed

| Command | Result |
|---|---|
| `dart format lib` in main app | Passed |
| `flutter analyze` in main app | Passed |
| `flutter test` in main app | Passed, 1 test |
| `dart format lib` in scanner app | Passed |
| `flutter analyze` in scanner app | Passed |
| `flutter test` in scanner app | Passed, 2 tests |
| `mvn compile` in backend | Passed |
| `mvn test` in backend | Passed, 15 tests |

Known build warning: Maven reports that the OVSE SDK is configured through a system-scoped JAR under `backend/libs`. This compiles locally but should be moved to an internal artifact repository or standard dependency management before long-term production maintenance.
