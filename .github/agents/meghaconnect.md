# MeghaConnect – Coding Agent Context

## Project Overview
**MeghaConnect** is the Chief Minister's Office (CMO) of Meghalaya scheduling, scheme management, and grievance portal. It consists of three tightly-coupled sub-projects:

| Sub-project | Technology | Location |
|---|---|---|
| `backend` | Spring Boot 3 (Java 21), JPA/Hibernate, MySQL, Flyway, JWT | `backend/` |
| `frontend` | Angular 19 (standalone), PrimeNG, TypeScript | `frontend/src/app/` |
| `mobile` | Flutter 3 (Dart), Provider, `http`, `shared_preferences` | `mobile/lib/` |

---

## Backend

### Spring Boot configuration
- Main class: `com.survisha.meghaconnect.MeghaConnectApplication`
- Config: `backend/src/main/resources/application.yml`
- JWT secret injected at `app.jwt.secret`; expiry at `app.jwt.expiration-ms`
- DB migrations: `backend/src/main/resources/db/migration/V*.sql` (Flyway)
- Security: stateless JWT (`SecurityConfig.java`). All `/api/v1/auth/**` and `/api/v1/public/**` routes are public; everything else requires a valid JWT.

### Key REST endpoints
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | none | `{username,password}` → `{token,username,fullName,role,expiresIn}` |
| POST | `/api/v1/public/otp/send` | none | `{phoneNumber}` → sends OTP |
| POST | `/api/v1/public/otp/verify` | none | `{phoneNumber,otp}` → `{registrationToken}` |
| POST | `/api/v1/public/register` | none | citizen registration |
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
`AuthService.login()` → calls `POST /api/v1/auth/login` → stores JWT in `sessionStorage('megha_token')` → `authInterceptor` picks it up for all subsequent requests. Role is stored without `ROLE_` prefix.

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
Staff login → `ApiService.login(username, password)` → stores JWT in shared_preferences (`megha_token`).
Public login → `ApiService.sendOtp(phone)` → `ApiService.verifyOtp(phone, otp)` → `auth.publicLogin(phone)` which calls `ApiService.login('public1','public123')`.

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
| Name | Hex | Usage |
|---|---|---|
| Primary Blue | `#1A237E` | Primary brand, headers, key actions |
| Action Blue | `#1565C0` | Secondary actions |
| Teal/Green | `#065F46` | Success, approved, scheme apps |
| Amber | `#B45309` | Warnings, CMO review, pending |
| Danger Red | `#DC2626` / `#991B1B` | Rejections, overdue, HCM_REJECTED |
| Grey | `#374151` | Body text |

---

## Flyway migration naming convention
Files live in `backend/src/main/resources/db/migration/`. Use sequential versioning:
- `V1__initial_schema.sql`
- `V2__seed_data.sql`
- `V3__extended_schema.sql`
- `V4__public_registration_kyc.sql`
- `V5__grievances.sql`
- Next: `V6__<description>.sql`

---

## Development workflow
1. Make backend changes → `mvn compile` to verify (run from `backend/`)
2. Add Flyway migration if schema changes
3. Make frontend/mobile changes in parallel
4. No `package.json` in `frontend/` – Angular node_modules are not committed
5. No `pubspec.lock` changes unless adding new dependencies (check advisory DB first)
