# MeghaConnect Android Release Deployment

## Android App Identity

| App | Display Name | Package ID |
|---|---|---|
| Main app | MeghaConnect | `in.gov.meghalaya.meghaconnect` |
| Scanner app | MeghaConnect Security | `in.gov.meghalaya.meghaconnect.securityscanner` |

Manual confirmation required: final package ownership must be approved before publishing. Use NITCON package IDs only if NITCON is the approved store publisher.

## Release Signing

Do not commit keystores, passwords, or `key.properties`.

Supported signing inputs:

```properties
storeFile=C:\\secure\\meghaconnect-upload-key.jks
storePassword=<secret>
keyAlias=<alias>
keyPassword=<secret>
```

CI environment variable alternative:

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
ANDROID_STORE_PASSWORD
```

## Main App AAB

```powershell
cd mobile
flutter build appbundle --release `
  --dart-define=MEGHA_API_BASE_URL=https://<production-api-host>/api `
  --dart-define=MEGHA_PRIVACY_POLICY_URL=https://<official-privacy-url> `
  --dart-define=MEGHA_TERMS_URL=https://<official-terms-url> `
  --dart-define=MEGHA_CONSENT_VERSION=2026-05-25
```

Release validation fails if `MEGHA_API_BASE_URL` is missing, non-HTTPS, or demo credentials are enabled.

## Scanner AAB

```powershell
cd meghaconnect_security_scanner
flutter build appbundle --release `
  --dart-define=APP_ENV=prod `
  --dart-define=API_BASE_URL=https://<production-api-host> `
  --dart-define=USE_MOCK_AUTH=false `
  --dart-define=USE_MOCK_QR=false
```

Release validation fails if `APP_ENV=prod` is missing, `API_BASE_URL` is missing/non-HTTPS, or mock flags are enabled.

## Pre-Submission Checklist

- Manual confirmation required: official package ownership.
- Manual confirmation required: release keystore generated and stored securely.
- Manual confirmation required: Play App Signing configured.
- Manual confirmation required: production API, privacy, and terms URLs approved.
- Manual confirmation required: final app icon, splash screen, screenshots, and metadata.
- Manual confirmation required: reviewer credentials and sample QR/pass.
- Manual confirmation required: Play Data Safety declaration completed.
- Manual confirmation required: SMS/WhatsApp, EPIC, OVSE, and CORS production settings verified.
