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
