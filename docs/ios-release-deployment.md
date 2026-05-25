# MeghaConnect iOS Release Deployment

## Current Status

iOS is **not included in this release**. The repository currently does not contain `mobile/ios` or `meghaconnect_security_scanner/ios`, so IPA/archive readiness cannot be verified from code.

## Required If iOS Is Approved Later

Generate or restore Flutter iOS projects:

```powershell
cd mobile
flutter create --platforms=ios .

cd ..\meghaconnect_security_scanner
flutter create --platforms=ios .
```

Manual confirmation required before generation because iOS bundle IDs and Apple signing ownership must be approved first.

## Bundle IDs

Recommended IDs if Government of Meghalaya is publisher:

| App | Bundle ID |
|---|---|
| Main app | `in.gov.meghalaya.meghaconnect` |
| Scanner app | `in.gov.meghalaya.meghaconnect.securityscanner` |

Use NITCON IDs only if NITCON is the approved Apple Developer publisher.

## Required iOS Configuration

- Apple Developer Team ID and signing certificates.
- App icons and launch screen.
- `NSCameraUsageDescription`.
- `NSPhotoLibraryUsageDescription` if photo library access is used.
- Document picker entitlement review if document upload is enabled.
- Privacy manifest/app privacy answers.
- Privacy Policy and Terms links in App Store Connect.
- Reviewer credentials and test instructions.
- Archive through Xcode or `flutter build ipa --release`.

## Recommendation

No Apple App Store submission should be planned until iOS projects are generated, signed, configured, archived, and tested on physical devices.
