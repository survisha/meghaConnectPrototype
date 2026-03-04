# agent-Narsingh – MeghaConnect Coding Agent

## Project Overview
**MeghaConnect** is the Chief Minister's Office (CMO) of Meghalaya scheduling, scheme management, and grievance portal. It consists of three tightly-coupled sub-projects:

| Sub-project | Technology | Location |
|---|---|---|
| `backend` | Spring Boot 2.7.18 (Java 1.8), JPA/Hibernate, MySQL, Flyway, JWT | `backend/` |
| `frontend` | Angular 19 (standalone), PrimeNG, TypeScript | `frontend/src/app/` |
| `mobile` | Flutter 3 (Dart), Provider, `http`, `shared_preferences` | `mobile/lib/` |

---

## Backend

### Spring Boot configuration
- Main class: `com.survisha.meghaconnect.MeghaConnectApplication`
- Config: `backend/src/main/resources/application.yml`
- JWT secret injected at `app.jwt.secret`; expiry at `app.jwt.expiration-ms`
- DB migrations: `backend/src/main/resources/db/migration/V*.sql` (Flyway)
- Security: stateless JWT (`SecurityConfig.java`). All `/api/v1/auth/**` and `/api/v1/visitor/auth/**` routes are public; everything else requires a valid JWT.

### Key REST endpoints

**Staff Authentication:**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | none | `{username,password}` → `{token,username,fullName,role,expiresIn}` |

**Visitor/Citizen Authentication (OTP-based):**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/visitor/auth/check-mobile` | none | `{phoneNumber}` → `{registered:boolean,message}` |
| POST | `/api/v1/visitor/auth/generate-otp` | none | `{phoneNumber}` → `{success,otp,message}` (OTP in response for demo) |
| POST | `/api/v1/visitor/auth/validate-otp` | none | `{phoneNumber,otp}` → `{success,token,fullName,visitorId,role}` |
| POST | `/api/v1/visitor/auth/register` | none | `{fullName,phoneNumber,email?,epicNumber?,aadhaarNumber?,address,...}` → `{success,visitorId,kycStatus}` |
| GET  | `/api/v1/visitor/auth/profile/{id}` | JWT | Get visitor profile → `{id,fullName,phoneNumber,kycType,kycVerified,...}` |

**Visitor KYC Validation (Multi-step, MOCK):**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/visitor/validate-id` | none | `{idType,idValue,phoneNumber}` → `{success,otpSent,otp,message}` - Validates EPIC/Aadhaar format and sends OTP (mock: OTP=123456) |
| POST | `/api/v1/visitor/verify-otp` | none | `{phoneNumber,otp,idType,idValue}` → `{success,verified,demographics:{fullName,address,district,constituency}}` - Verifies OTP and returns mock demographics |
| POST | `/api/v1/visitor/validate-face` | none | `{idType,idValue,livePhotoBase64}` → `{success,matched,kycStatus,confidence,message}` - Validates face photo (mock: always returns PHOTO_MATCHED) |

**Core Application APIs:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET  | `/api/v1/appointments` | JWT | paginated list |
| POST | `/api/v1/appointments` | JWT | create appointment (`AppointmentDto`) |
| GET  | `/api/v1/appointments/{id}` | JWT | single appointment |
| PATCH| `/api/v1/appointments/{id}/status` | JWT (staff) | `{status,remarks?}` |
| POST | `/api/v1/appointments/{id}/schedule` | JWT (staff) | `{scheduledDateTime,durationMinutes}` |
| GET  | `/api/v1/grievances` | JWT | paginated list |
| POST | `/api/v1/grievances` | JWT | create grievance |
| PATCH| `/api/v1/grievances/{id}/status` | JWT | `{status,remarks?}` |
| GET  | `/api/v1/schedule` | JWT | list of `ScheduleEvent` |
| POST | `/api/v1/schedule` | JWT (staff) | create event |
| GET  | `/api/v1/audit-logs` | JWT (ADMIN/HCM/OSD) | paginated audit log |
| GET  | `/api/v1/persons/search/phone/{phone}` | JWT | single person or 404 |
| GET  | `/api/v1/persons/search/epic/{epic}` | JWT | single person or 404 |
| GET  | `/api/v1/persons/search/name?q=` | JWT | list of persons |
| GET  | `/api/v1/persons/search/district/{d}` | JWT | list of persons |
| GET  | `/api/v1/directions` | JWT | all directions (follow-ups) |
| GET  | `/api/v1/users` | JWT (ADMIN/HCM/OSD) | all users |
| POST | `/api/ai/generate-summary` | none | `{appointmentId?,agendaBrief,agendaType,applicantName,district}` |

### Spring page response format
```json
{ "content": [...], "totalElements": 5, "totalPages": 1, "size": 50, "number": 0 }
```

### Key entity enums
- `Appointment.AppointmentStatus`: `SUBMITTED`, `DEO_PROCESSED`, `CMO_REVIEW`, `APPROVER_REVIEW`, `HCM_PENDING`, `HCM_ACCEPTED`, `HCM_SNOOZED`, `HCM_REJECTED`, `SCHEDULED`, `COMPLETED`, `CANCELLED`
- `Appointment.EventType`: `A1`, `A2`, `A3`, `A4`, `B1`, `B2`
- `Appointment.MeetingLocation`: `SHILLONG`, `TURA`, `DELHI`, `OTHERS`
- `Grievance.GrievanceStatus`: `SUBMITTED`, `ACKNOWLEDGED`, `UNDER_REVIEW`, `FORWARDED`, `RESOLVED`, `CLOSED`
- `Grievance.GrievanceCategory`: `PUBLIC_SERVICES`, `INFRASTRUCTURE`, `HEALTH`, `EDUCATION`, `EMPLOYMENT`, `WELFARE_SCHEME`, `LAW_ORDER`, `OTHERS`
- `User.UserRole`: `HCM`, `ADMIN`, `SAIDUL_OSD`, `APPROVER_JT_SECY`, `CMO_OFFICER`, `DATA_ENTRY_OPERATOR`, `PUBLIC`
- `Direction.DirectionColor`: `GREEN`, `YELLOW`, `BLUE`
- `Person.KycStatus`: `PENDING`, `PHOTO_MATCHED`, `DEMOGRAPHIC_MATCHED`, `FAILED`, `NOT_VERIFIED` (added in V7)

### Demo users (seeded by Flyway V2)
| Username | Password | Role |
|---|---|---|
| `hcm` | `hcm123` | HCM |
| `admin` | `admin123` | ADMIN |
| `saidul` | `osd123` | SAIDUL_OSD |
| `jtsecy` | `jts123` | APPROVER_JT_SECY |
| `cmo` | `cmo123` | CMO_OFFICER |
| `deo1` | `deo123` | DATA_ENTRY_OPERATOR |
| `public1` | `public123` | PUBLIC |

### Visitor/Citizen Authentication Flow

**Registration (self-service):**
1. Visitor fills registration form at `/register-visitor`
2. Required: Full name, 10-digit mobile number
3. Optional: Email, EPIC/Aadhaar, address, district, constituency
4. **KYC Validation Rules:**
   - EPIC: Must be 3 uppercase letters + 7 digits (e.g., `ABC1234567`)
   - Aadhaar: Must be exactly 12 digits
   - Duplicate mobile check performed
5. Record saved with `kycVerified=false` and `kycStatus=PENDING`
6. Redirect to `/public-login`

**OTP Login (two-step):**
1. **Step 1 - Mobile Entry:** Visitor enters mobile at `/public-login`
   - System calls `/api/v1/visitor/auth/check-mobile`
   - If not registered → show "Account not found" message
   - If registered → generate OTP
2. **Step 2 - OTP Validation:** Visitor enters 6-digit OTP
   - System calls `/api/v1/visitor/auth/validate-otp`
   - On success → JWT token issued → stored in sessionStorage
   - Redirect to `/visitor` dashboard

**Security Controls:**
- OTP validity: 5 minutes
- Max OTP attempts: 5 (then locked, must regenerate)
- Rate limiting: Max 10 OTP requests per hour per phone
- OTP replay protection: Each OTP can only be used once (consumed flag)
- Database table: `visitor_otp_temp` (V6 migration)

**Session Storage:**
- `megha_token` - JWT bearer token
- `megha_user` - `{username, fullName, role: 'PUBLIC'}`
- `megha_visitor_id` - Person ID

**TODO:** SMS gateway integration (currently OTP returned in API response for demo)

### Enhanced KYC Validation Flow (Multi-step Registration)

**Overview:**
New visitor registration on `/register-visitor` uses a 4-step KYC validation flow with ID verification, OTP validation, and live photo capture with face matching.

**Flow Steps:**
1. **ID Entry (Step 1):** Visitor selects ID type (EPIC/Aadhaar) and enters ID number + mobile
   - Frontend validates format (EPIC: 3 letters + 7 digits, Aadhaar: 12 digits)
   - Calls `POST /api/v1/visitor/validate-id` → backend validates format and sends OTP (mock: always "123456")
   - Progress: `id-entry` → `otp-verification`

2. **OTP Verification (Step 2):** Visitor enters 6-digit OTP
   - Calls `POST /api/v1/visitor/verify-otp` → backend validates OTP (accepts "123456")
   - On success: Returns mock demographics data (fullName, address, district, constituency)
   - Frontend auto-populates form fields with demographics
   - Progress: `otp-verification` → `photo-capture`

3. **Photo Capture (Step 3):** Visitor captures live photo via camera
   - Frontend uses `navigator.mediaDevices.getUserMedia()` to access camera
   - Photo captured as base64-encoded JPEG
   - Calls `POST /api/v1/visitor/validate-face` with `{idType, idValue, livePhotoBase64}`
   - Backend returns face validation result (mock: always PHOTO_MATCHED with 95.5% confidence)
   - Progress: `photo-capture` → `kyc-complete`

4. **Complete Registration (Step 4):** Final review and submission
   - All fields displayed (demographics read-only, email editable)
   - KYC status badge shown (green for PHOTO_MATCHED, yellow for DEMOGRAPHIC_MATCHED)
   - Calls `POST /api/v1/visitor/auth/register` with complete data + `kycStatus`
   - Record saved with `kycVerified=true` (if PHOTO_MATCHED) or `false` (if DEMOGRAPHIC_MATCHED)
   - Redirect to `/public-login`

**KYC Status Logic:**
- `PHOTO_MATCHED`: OTP verified + live photo matched with ID photo (face recognition success) → `kycVerified=true`
- `DEMOGRAPHIC_MATCHED`: OTP verified but no photo match or photo capture skipped → `kycVerified=false`
- `FAILED`: OTP invalid or face recognition failed → registration blocked
- `PENDING`: Default state for existing records (pre-V7 data)

**Mock Behavior (Demo Only):**
- OTP is always "123456" for successful verification
- EPIC/Aadhaar validation is format-only (no API integration)
- Face matching always returns PHOTO_MATCHED (no actual face recognition)
- Demographics returned are hard-coded sample data:
  - EPIC → "Rajesh Kumar Sharma, Laitumkhrah, East Khasi Hills, Shillong North"
  - Aadhaar → "Priya Singh, Police Bazar, East Khasi Hills, Shillong Central"

**Database Schema (V7 Migration):**
- `persons` table additions:
  - `photo_from_id_base64` (LONGTEXT) - Photo from ID card
  - `live_photo_base64` (LONGTEXT) - Live captured photo
  - `kyc_status` (VARCHAR(50)) - PENDING | PHOTO_MATCHED | DEMOGRAPHIC_MATCHED | FAILED | NOT_VERIFIED

**Production TODO:**
- Integrate Election Commission API for EPIC verification
- Integrate UIDAI API for Aadhaar verification
- Implement actual SMS gateway for OTP delivery
- Implement face recognition service (AWS Rekognition, Azure Face API, etc.)
- Store images in S3/Azure Blob (not database) and keep only URLs
- Add audit trail for all KYC validation attempts

### How to run backend
```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
```

---

## Frontend (Angular)

### Structure
- `frontend/src/app/services/` – all HTTP services
- `frontend/src/app/models/index.ts` – shared TypeScript models
- `frontend/src/app/interceptors/auth.interceptor.ts` – attaches JWT to all outgoing requests
- `frontend/src/app/app.config.ts` – providers including `provideHttpClient(withInterceptors([authInterceptor]))`
- Proxy: API calls go to `/api/...` which Angular's dev server proxies to `localhost:8080`

### Services
| Service | File | Description |
|---|---|---|
| `AuthService` | `services/auth.service.ts` | Real login, JWT storage, role checks |
| `AppointmentService` | `services/appointment.service.ts` | CRUD + status + schedule |
| `GrievanceService` | `services/grievance.service.ts` | List, create, update status |
| `AuditLogService` | `services/audit-log.service.ts` | Paginated audit log |
| `PersonService` | `services/person.service.ts` | Search by phone/EPIC/name/district |
| `ScheduleEventService` | `services/schedule-event.service.ts` | CRUD for schedule events |
| `SchemeService` | `services/scheme.service.ts` | Scheme applications |

### Auth flow

**Staff Login:**
`AuthService.login()` → calls `POST /api/v1/auth/login` → stores JWT in `sessionStorage('megha_token')` → `authInterceptor` picks it up for all subsequent requests. Role is stored without `ROLE_` prefix.

**Visitor/Citizen Login:**
`PublicLoginComponent` →Two-step OTP flow:
1. Check mobile → Generate OTP → `/api/v1/visitor/auth/generate-otp`
2. Validate OTP → `/api/v1/visitor/auth/validate-otp` → JWT returned
3. `AuthService.setVisitorSession()` stores token and user data
4. Navigate to `/visitor` (protected by `roleGuard('PUBLIC')`)

**Components:**
- `/login` - Staff login (username/password) with mode toggle for citizen redirect
- `/public-login` - Citizen OTP login (two-step)
- `/register-visitor` - Citizen self-registration
- `/visitor` - Visitor dashboard (appointments, grievances, schemes)

### UserRole type
`'HCM' | 'ADMIN' | 'SAIDUL_OSD' | 'APPROVER_JT_SECY' | 'CMO_OFFICER' | 'DATA_ENTRY_OPERATOR' | 'PUBLIC'`

---

## Mobile (Flutter)

### Structure
- `mobile/lib/services/api_service.dart` – central HTTP client (base URL, token, all endpoint methods)
- `mobile/lib/services/auth_service.dart` – Provider-based auth; calls `ApiService.login()`
- `mobile/lib/services/navigation_service.dart` – simple route-string state
- `mobile/lib/models/user.dart` – `AuthUser`, `UserRole` enum
- `mobile/lib/screens/` – all screens

### ApiService base URL
```dart
static const String baseUrl = 'http://localhost:8080';
```
Change this for production deployments.

### Auth flow

**Staff login:**
`ApiService.login(username, password)` → stores JWT in shared_preferences (`megha_token`).

**Visitor/Citizen login:**
Two-step OTP flow in `login_screen.dart`:
1. Enter mobile → `/api/v1/visitor/auth/check-mobile` → `/api/v1/visitor/auth/generate-otp`
2. Enter 6-digit OTP → `/api/v1/visitor/auth/validate-otp` → JWT returned
3. Store token in shared_preferences → navigate to visitor dashboard

APIs match backend `/api/v1/visitor/auth/**` endpoints.

### All screens mapped to API
| Screen | API used |
|---|---|
| `login_screen.dart` | `ApiService.login()` + OTP endpoints |
| `dashboard_screen.dart` | `getScheduleEvents()` + `getAuditLogs()` |
| `appointments_screen.dart` | `getAppointments()` |
| `approver_screen.dart` | `getAppointments()` + `updateAppointmentStatus()` |
| `grievance_screen.dart` | `getGrievances()` + `createGrievance()` + `updateGrievanceStatus()` |
| `schemes_screen.dart` | `getSchemeApplications()` |
| `calendar_screen.dart` | `getScheduleEvents()` |
| `audit_trail_screen.dart` | `getAuditLogs()` |
| `public_identification_screen.dart` | `searchPersonByPhone/Epic/Name/District()` |
| `pending_followups_screen.dart` | `getDirections()` |
| `user_management_screen.dart` | `getUsers()` |
| `visitor_dashboard_screen.dart` | `getAppointments()` + `getSchemeApplications()` + `getGrievances()` |

---

## Design System / Color Palette (both frontend and mobile use these)

### Color Palette
| Name | Hex | Usage |
|---|---|---|
| Primary Blue | `#1A237E` | Primary brand, headers, key actions |
| Action Blue | `#1565C0` | Secondary actions |
| Teal/Green | `#065F46` | Success, approved, scheme apps |
| Amber | `#B45309` | Warnings, CMO review, pending |
| Danger Red | `#DC2626` / `#991B1B` | Rejections, overdue, HCM_REJECTED |
| Grey | `#374151` | Body text |

### UI/UX Business Rules

**1. Form & Input Backgrounds (Mandatory):**
- All form fields, input boxes, textareas, and select dropdowns **MUST** have white (`#FFFFFF`) background
- All tables and data grids **MUST** have white background for rows
- All dialog/modal content areas **MUST** have white background
- Hover states may use light grey (`#F8FAFC`, `#F3F4F6`) but never dark/black backgrounds
- This ensures readability and consistency across the application

**Required CSS for Custom Input Fields:**
```scss
.input-field {
  background: white;        // Mandatory white background
  color: #1f2937;          // Dark grey text for readability
  border: 1.5px solid #d1d5db;
  // ... other styles
}
```
- PrimeNG components automatically have white backgrounds via global styles
- Custom input classes (e.g., `.input-field`) must explicitly set `background: white` and `color: #1f2937`

**2. Table Styling:**
- Table headers: Primary blue gradient (`#1A237E` to `#3949AB`) with white text
- Table rows: White background (`#FFFFFF`)
- Alternating rows (optional): Light grey (`#F9FAFB`)
- Hover state: Slightly darker grey (`#F1F5F9`)

**3. Card Components:**
- All cards **MUST** have white background
- Card headers may use colored backgrounds (primary blue gradient)
- Card body/content always white background

---

## Flyway migration naming convention
Files live in `backend/src/main/resources/db/migration/`. Use sequential versioning:
- `V1__initial_schema.sql` - Core tables (appointments, persons, users, etc.)
- `V2__seed_data.sql` - Demo staff users (hcm, admin, saidul, etc.)
- `V3__extended_schema.sql` - Extended schema
- `V4__public_registration_kyc.sql` - Public registration and KYC fields
- `V5__grievances.sql` - Grievances table + public demo user
- `V6__visitor_otp_auth.sql` - Visitor OTP temp table (`visitor_otp_temp`) + email column in persons
- `V7__kyc_enhanced_validation.sql` - KYC photo fields (`photo_from_id_base64`, `live_photo_base64`, `kyc_status`) for multi-step validation
- Next: `V8__<description>.sql`

---

## Development workflow
1. Make backend changes → `mvn compile` to verify (run from `backend/`)
2. Add Flyway migration if schema changes
3. Make frontend/mobile changes in parallel
4. No `package.json` in `frontend/` – Angular node_modules are not committed
5. No `pubspec.lock` changes unless adding new dependencies (check advisory DB first)
