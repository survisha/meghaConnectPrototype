# MeghaConnect – Flutter Mobile App

Mobile app for the **Chief Minister's Office of Meghalaya** built with Flutter, mirroring the Angular web frontend.

## Prerequisites

- [Flutter SDK](https://docs.flutter.dev/get-started/install) ≥ 3.0.0
- Dart ≥ 3.0.0
- Android Studio / Xcode (for emulator/device)

## Getting Started

```bash
# Navigate to the mobile folder
cd mobile

# Install dependencies
flutter pub get

# Run on connected device or emulator
flutter run

# Build APK (Android)
flutter build apk

# Build for iOS
flutter build ios
```

## iOS Testing

iOS builds require macOS with Xcode and CocoaPods installed. The generated iOS
workspace is `ios/Runner.xcworkspace`.

Current iOS configuration:

- Bundle identifier: `in.gov.meghalaya.meghaconnect`
- Display name: `Megha Connect`
- Minimum iOS version: `12.0`
- Signing: automatic signing is enabled, but the Apple development team must be
  selected in Xcode before running on a physical iPhone or creating an archive.
- Debug HTTP: local network HTTP is allowed only for Debug builds. Release and
  production API URLs must use HTTPS.

Simulator:

```bash
flutter run -d ios --dart-define=MEGHA_API_BASE_URL=http://localhost:8080/api
```

Physical iPhone:

```bash
flutter devices
flutter run -d <device-id> --dart-define=MEGHA_API_BASE_URL=http://<LAN-IP>:8080/api
```

Debug build:

```bash
flutter build ios --debug
```

Release build:

```bash
flutter build ios --release --dart-define=MEGHA_API_BASE_URL=https://your-api-host/api
```

Before testing on iOS:

```bash
flutter doctor
flutter clean
flutter pub get
flutter build ios --debug
```

Testing checklist:

- [ ] Simulator test completed
- [ ] Real iPhone test completed
- [ ] Camera permission tested
- [ ] Photo/file attachment tested
- [ ] Face ID/Touch ID tested for offline login
- [ ] API connectivity tested with the correct simulator or LAN URL
- [ ] Offline mode tested
- [ ] Sync pending screen tested
- [ ] UI responsiveness tested on small and large iPhones
- [ ] App does not crash during login, capture, upload, offline, or sync flows

## Android Release Build

Release APK/AAB builds require a signing keystore and a production API URL.

1. Create or obtain the release keystore.
2. Copy `android/key.properties.example` to `android/key.properties`.
3. Fill `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.
4. Build with an HTTPS API URL:

```bash
flutter build apk --release --dart-define=MEGHA_API_BASE_URL=https://your-api-host/api
```

For CI, provide these environment variables instead of `key.properties`:
`ANDROID_KEYSTORE_PATH`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and
`ANDROID_STORE_PASSWORD`.

For local testing without release signing, use:

```bash
flutter build apk --debug
```

## Review / UAT Credentials

Do not commit production or demo passwords. For UAT/reviewer access, provide
temporary credentials out of band and enable quick-login helpers only in
non-production builds with `--dart-define=ENABLE_DEMO_CREDENTIALS=true`.

## App Structure

```
lib/
├── main.dart                   # App entry point, theme, providers
├── models/
│   └── user.dart               # UserRole enum, AuthUser class
├── services/
│   ├── auth_service.dart       # Authentication (ChangeNotifier)
│   └── navigation_service.dart # In-app navigation state
└── screens/
    ├── login_screen.dart       # Staff + Citizen login tabs
    ├── main_shell.dart         # Navigation drawer shell
    ├── dashboard_screen.dart   # Role-filtered KPI dashboard
    ├── appointments_screen.dart# Appointment list + search
    ├── new_appointment_screen.dart # New/Walk-in appointment form
    └── user_management_screen.dart # User CRUD (Full Control only)
```

## Design

- **Primary colour**: `#1A237E` (Government of India deep blue)
- **Accent**: `#1565C0`, `#065F46` (green), `#B45309` (amber)
- Material 3 design with role-based access control
- Drawer navigation filtered by user role
- Public citizens login with phone + OTP and access only the appointment form

## Role Access Matrix

| Feature              | HCM | ADMIN | OSD | JT.SECY | CMO | DEO | PUBLIC |
|----------------------|:---:|:-----:|:---:|:-------:|:---:|:---:|:------:|
| Dashboard            | ✓   | ✓     | ✓   | ✓       | ✓   | ✓   | –      |
| Calendar/Schedule    | ✓   | ✓     | ✓   | ✓       | ✓   | –   | –      |
| All Appointments     | ✓   | ✓     | ✓   | ✓       | ✓   | ✓   | –      |
| New Appointment      | –   | ✓     | ✓   | –       | –   | ✓   | ✓      |
| Walk-in Counter      | –   | ✓     | ✓   | –       | –   | ✓   | –      |
| CM Schemes           | ✓   | ✓     | ✓   | ✓       | ✓   | –   | –      |
| Public Identification| ✓   | ✓     | ✓   | –       | –   | ✓   | –      |
| Reports              | ✓   | ✓     | ✓   | ✓       | ✓   | –   | –      |
| Audit Trail          | –   | ✓     | –   | –       | –   | –   | –      |
| User Management      | ✓   | ✓     | ✓   | –       | –   | –   | –      |
