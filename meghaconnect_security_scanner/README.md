# MeghaConnect Security Scanner

Separate Flutter Android project for MeghaConnect security staff. The app logs in with MeghaConnect user credentials, scans secure visitor QR tokens, validates them through the MeghaConnect Spring Boot backend, and performs visitor check-in/check-out.

This project is intentionally a separate top-level app folder and is not inside the existing Angular `frontend` project or the Spring Boot `backend` project.

## Features

- Security/Admin login via `POST /api/v1/auth/login`
- JWT stored with Android encrypted shared preferences
- Role gate for `SECURITY` and `ADMIN` users only
- Dashboard with user, gate, location, scan, recent scans, and logout
- Android camera QR scanner using `mobile_scanner`
- QR validation through `POST /api/v1/qr/validate`
- Visitor details screen with photo URL/path support
- Check-in through `POST /api/v1/qr/check-in`
- Check-out through `POST /api/v1/qr/check-out`
- In-memory recent scans cleared on logout
- Auto logout when JWT expiry is reached
- HTTPS-only API configuration and Android cleartext traffic disabled
- Stable generated `deviceId` stored securely for future device registration

## Project Structure

```text
lib/
  core/
    config/       Environment and base URL config
    network/      Dio client, JWT interceptor, API errors
    security/     Secure session storage and JWT expiry decode
    utils/        Device id, dates, JSON helpers
  data/
    api/          REST and mock API implementations
    models/       Auth, QR, visitor, recent scan models
    repositories/ Auth, QR, and recent scan repositories
  presentation/
    screens/      Login, dashboard, scanner, visitor details, recent scans
    state/        App state/session controller
    widgets/      Shared display widgets
```

## Backend API Configuration

The app reads configuration through Dart defines:

| Define | Default | Notes |
| --- | --- | --- |
| `APP_ENV` | `dev` | `dev`, `uat`, or `prod` |
| `API_BASE_URL` | Environment placeholder | Must be HTTPS |
| `USE_MOCK_AUTH` | `false` | Set `true` for offline login demos |
| `USE_MOCK_QR` | `false` | Legacy flag retained for older builds; current app uses the backend visitor-pass API |
| `USE_BACKEND_RECENT_SCANS` | `false` | Set `true` if `GET /api/v1/qr/recent-scans` exists |

Example run against UAT:

```powershell
flutter run --dart-define=APP_ENV=uat --dart-define=API_BASE_URL=https://uat-api.meghaconnect.gov.in --dart-define=USE_MOCK_QR=false
```

Example local demo with mock auth and mock QR:

```powershell
flutter run --dart-define=USE_MOCK_AUTH=true --dart-define=USE_MOCK_QR=true
```

## Expected Backend APIs

### Login

`POST /api/v1/auth/login`

Request:

```json
{
  "username": "security.user",
  "password": "secret"
}
```

Response fields consumed by the app:

```json
{
  "token": "jwt",
  "refreshToken": "optional",
  "username": "security.user",
  "fullName": "Security User",
  "role": "SECURITY",
  "expiresIn": 28800
}
```

### Security Profile

`GET /api/v1/security/profile`

Optional response fields:

```json
{
  "username": "security.user",
  "fullName": "Security User",
  "role": "SECURITY",
  "gateName": "Main Gate",
  "location": "Secretariat Entry"
}
```

### QR Validate

`POST /api/v1/visitor-pass/validate`

Request:

```json
{
  "qrData": "secure-token-only",
  "deviceId": "generated-device-id",
  "gateName": "Main Gate",
  "location": "Secretariat Entry"
}
```

Response fields consumed by the app:

```json
{
  "visitorName": "Visitor Name",
  "visitorPhotoUrl": "https://...",
  "appointmentId": "APT-1001",
  "appointmentDateTime": "2026-05-14T10:30:00+05:30",
  "purpose": "Official meeting",
  "department": "General Administration Department",
  "personToMeet": "Officer Name",
  "qrStatus": "VALID",
  "entryExitStatus": "NOT_CHECKED_IN",
  "canCheckIn": true,
  "canCheckOut": false
}
```

### Check-In / Check-Out

`POST /api/v1/visitor-pass/check-in`

`POST /api/v1/qr/check-out`

Request:

```json
{
  "qrToken": "secure-token-only",
  "deviceId": "generated-device-id",
  "gateName": "Main Gate",
  "location": "Secretariat Entry"
}
```

The app handles invalid QR, expired QR, already checked-in, already checked-out, cancelled appointment, 401, and 403 responses through backend status/message fields.

## Build

Install dependencies:

```powershell
flutter pub get
```

Analyze and test:

```powershell
flutter analyze
flutter test
```

Build APK:

```powershell
flutter build apk --release --dart-define=APP_ENV=prod --dart-define=API_BASE_URL=https://api.meghaconnect.gov.in --dart-define=USE_MOCK_QR=false
```

## Security Notes

- QR codes must contain only the secure QR token, never visitor personal data.
- JWT/session data is stored in encrypted shared preferences.
- Visitor details and recent scans are kept only in app memory and cleared on logout.
- Android cleartext traffic is disabled and `API_BASE_URL` must use HTTPS.
- The generated device id is retained securely and sent with QR validation/actions.
- Backend should add/allow a `SECURITY` role for scanner users, while `ADMIN` remains allowed.
