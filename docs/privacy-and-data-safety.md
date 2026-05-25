# MeghaConnect Privacy and Data Safety

## Consent Implementation

The mobile app now captures consent before citizen registration, public appointment submission, and guest appointment submission. Backend fields persist:

- `consentAccepted`
- `consentVersion`
- `consentTimestamp`
- `privacyPolicyUrl`
- `termsUrl`

Manual confirmation required: official Privacy Policy, Terms & Conditions, consent wording, retention policy, and deletion/escalation process must be approved by the competent authority.

## Data Collection Matrix

| Data Type | Purpose | Stored Where | Shared With | Retention | Security Control |
|---|---|---|---|---|---|
| Name | Registration, appointment, scheme review | Backend DB | Authorized staff | Manual policy required | RBAC, audit logging |
| Mobile number | OTP, notifications, contact | Backend DB | SMS/WhatsApp gateway | Manual policy required | OTP rate limiting, masking |
| EPIC reference | KYC, duplicate prevention | Backend DB | EPIC provider | Manual policy required | Format validation, masking recommended |
| Aadhaar reference/OVSE claims | KYC fallback | Backend DB / OVSE callback flow | OVSE provider | Manual policy required | Secret-based provider config, masking required |
| Photo | KYC/entry/security | Backend file store | Authorized staff, future face API | Manual policy required | Authenticated access, AV hook |
| Documents | Scheme/appointment support | Backend file store | Authorized staff, OCR/AI services | Manual policy required | File validation, auth streaming, AV hook |
| Appointment details | Governance workflow | Backend DB | CMO/HCM/Approver/DEO | Manual policy required | JWT/RBAC, IDOR checks |
| QR scan data | Entry control and audit | Backend DB | SECURITY/ADMIN | Manual policy required | Role-limited APIs, replay checks |
| Session token | Authentication | Device secure storage | Backend as bearer token | Until logout/expiry | `flutter_secure_storage` |
| Consent metadata | Compliance evidence | Backend DB | Authorized auditors/staff | Manual policy required | Version/timestamp capture |

## Google Play Data Safety

Manual submission required. Declare collection/use for:

- Personal info: name, phone, address, email where applicable.
- Photos and videos: live photo/citizen photo.
- Files and docs: uploaded scheme/appointment documents.
- App activity: appointment and workflow activity.
- Device or other IDs: only if analytics/device identifiers are later enabled.

Declare whether each data type is encrypted in transit, whether deletion can be requested, and whether sharing occurs with SMS/WhatsApp/KYC/OCR/AI service providers.

## Apple App Privacy

Not applicable until iOS is included. If iOS is added, App Store Connect privacy labels must match the same collection categories above.

## Manual Policy Items

- Retention/deletion periods.
- Aadhaar/EPIC masking requirements.
- Data sharing agreements with SMS, WhatsApp, EPIC, OVSE, OCR, AI, hosting, and storage providers.
- Citizen grievance/contact channel for privacy requests.
- Whether screenshot prevention/root detection/certificate pinning are mandatory controls.
