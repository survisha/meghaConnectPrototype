# MeghaConnect Application Discovery and Manual Regression Catalog

Generated from the current Angular, Flutter, and Spring Boot source tree. This is a selectable source catalog for later import into the Regression Scripts workbook. Application code was not changed.

## Discovery summary

| Item | Discovered |
|---|---:|
| Angular routed pages | 29 |
| Angular components scanned | 44 |
| Flutter screens | 25 |
| Spring REST controllers | 39 |
| Backend appointment states | 28 |
| Primary implemented roles | 11 |

### Angular route inventory

Public routes: Home, Staff Login, Change Password, Public Login, Visitor Registration, Guest Appointment, Department Access Request.

Authenticated routes: Dashboard, Visitor Dashboard, Scheduling, Appointments, New Appointment, Walk-in Counter, Walk-in Appointments, Pending Approvals, Approval Details, Schemes, Scheme Application, Grievances, DEO Visitor Registration, Public Identification, Reports, Heatmap, Audit Trail, Legacy Data Import, Completed Appointments, Rejected Appointments, Closed Appointments, Department Management, Department Requests, User Management, Scheme Management, Appointment Type Management, HCM Appointments.

Embedded/shared functionality: Appointment Detail, CMO Review dialog, visitor history dialog, authenticated photos, camera capture/liveness, AI chatbot, AI insights, footer, language selector, toast, profile card.

### Flutter screen inventory

Login, Change Password, Main Shell, Dashboard, DEO Home, Visitor Dashboard, Appointments, New Appointment, Approver, HCM Dashboard, Calendar, Public Identification, Visitor Registration, Guest Appointment, Department Access Request, Schemes, Scheme Application, Grievances, Reports, Heatmap, Audit Trail, User Management, QR Scanner, Document Viewer, Pending Sync.

Mobile-specific implementation includes secure storage, connectivity monitoring, local database, offline repository, pending synchronization, offline AI-note caching, authenticated photos, and QR scanning.

### Backend controller inventory

AI, AI Summary, Appointment, Appointment Approval, Appointment Workflow, Appointment Report, Executive Appointment Report, Appointment Type, Audit Log, Authentication, CAPTCHA, Department, Department Access Request, Direction, Direction Follow-up, EPIC Face, Face Recognition, File Upload, Form Extraction, Grievance, Guest Appointment, HCM Action, HCM Decision Support, KYC, Legacy Dataset, Legacy Import, Legacy Person Search, Public Identification, QR Scanner, Reference Data, Role, Schedule Event, Scheme, Scheme Application, User, Visitor, Visitor Appointment, Visitor Authentication, Visitor Pass.

### Roles discovered

`SUPER_ADMIN`, `DEPARTMENT_ADMIN`, `DEO`, `DEPARTMENT_PA`, `HEAD_DEPARTMENT`, `HCM`, `ADMIN`, `APPROVER`, `SECURITY`/mobile `SECURITY_POLICE`, `PUBLIC`, and compatibility role `CITIZEN`. `APPROVER_JT_SECY` remains referenced in some authorization checks but migration V14 maps it to `APPROVER`; treat this as a compatibility/risk item.

### Appointment and related states discovered

Appointment: `PENDING`, `HCM_MET_COMPLETED`, `ROUTED_TO_OFFICIAL`, `CREATED`, `PENDING_APPROVER_REVIEW`, `FOLLOWUP`, `SELECTED_FOR_PUBLIC_DARBAR`, `PUBLIC_DARBAR_DATE_CREATED`, `SCHEDULED_FOR_PUBLIC_DARBAR`, `APPROVED`, `APPROVED_WITH_DATE_TIME`, `REJECTED`, `SUBMITTED`, `DEO_PROCESSED`, `CMO_REVIEW`, `APPROVER_REVIEW`, `HCM_PENDING`, `HCM_ACCEPTED`, `HCM_SNOOZED`, `HCM_REJECTED`, `SCHEDULED`, `FORWARDED_TO_DEPARTMENT`, `SUPPORTING_DOCUMENT_REQUIRED`, `COMPLETED`, `PENDING_REQUEST`, `RESCHEDULED`, `CLOSED`, `CANCELLED`.

Walk-in: `PENDING`, `COMPLETED`. Direction follow-up: `PENDING`, `IN_PROGRESS`, `COMPLETED`. Department access: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.

## Angular / Mobile synchronization matrix

| Feature | Angular | Mobile | Backend | Notes |
|---|---|---|---|---|
| Staff/public authentication | Yes | Yes | Yes | OTP visitor flow and forced password change exist |
| Dashboard | Yes | Yes | Yes | Role-specific variants |
| Visitor registration | Yes | Yes | Yes | Camera, KYC/EPIC and form extraction hooks |
| Public identification / face | Yes | Yes | Yes | Full-history API is authenticated |
| Appointments | Yes | Yes | Yes | Mobile and web action depth differs by role |
| Walk-in counter/list | Yes | Partial | Yes | Mobile DEO workflow is integrated rather than separate routed pages |
| Scheduling/calendar | Yes | Yes | Yes | Staff-role restrictions apply |
| Approval/HCM workflow | Yes | Yes | Yes | Rich state/action surface |
| Reports/heatmap/audit | Yes | Yes | Yes | Executive report scope differs from department reports |
| Schemes/application | Yes | Yes | Yes | Admin scheme configuration is web-oriented |
| Grievances | Yes | Yes | Yes | Public and staff behavior |
| Department management/requests | Yes | Request only | Yes | Department administration pages are Angular-only |
| User management | Yes | Yes | Yes | Backend restricts to super/department admin |
| Legacy import/search | Yes | No dedicated screen | Yes | Angular-only dedicated import UI |
| QR check-in/out | No dedicated scanner | Yes | Yes | Mobile/security-focused |
| Offline sync/pending queue | No | Yes | Indirect | Mobile-only |
| AI notes/summary | Yes | Cached notes support | Yes | Availability and partial failure require testing |

## Role coverage matrix

| Role | Main access | Major actions | Negative authorization focus |
|---|---|---|---|
| SUPER_ADMIN | All administrative and cross-department areas | Departments, requests, users, configuration, workflows | Protected bootstrap identity and destructive user operations |
| DEPARTMENT_ADMIN | Department-scoped users/reports/follow-ups | Manage permitted department roles and users | Cannot manage other departments or SUPER_ADMIN |
| DEPARTMENT_PA | Department appointments/reports/follow-ups | View/update scoped workflow | Cannot access global administration |
| HEAD_DEPARTMENT | Department-scoped account | Role-specific permitted work | No inferred access beyond policy |
| DEO | Registration, identification, walk-in, appointments | Register visitor, create/process appointments, QR validate | Cannot call admin or HCM-only APIs |
| APPROVER | Approval inbox, appointments, reports, schemes | Review, route, schedule, reject, remarks | Cannot perform super-admin configuration |
| HCM | HCM dashboard, approval/workflow, reports | Accept, important, modify, snooze, reject, directions | Cannot manage users/departments unless separately authorized |
| ADMIN | Operational configuration and selected workflows | Users/configuration where policy permits | Cannot use SUPER_ADMIN-only department APIs |
| SECURITY | QR/pass APIs | Validate, check-in/out | No appointment administration or PII browsing |
| PUBLIC/CITIZEN | Visitor dashboard, own appointments/schemes/grievances | OTP login, create own records, upload requested info | Cannot access another visitor or staff APIs |

## Regression Scripts

All cases have `Active = Yes`. Default assignment rotates across `Tester 1`, `Tester 2`, and `Tester 3`.

| Test Case ID | Module | Scenario / Test Case | Test Steps | Expected Result | Priority | Default Assigned To | Active |
|---|---|---|---|---|---|---|---|
| AUTH-001 | Authentication | Valid staff login | 1. Open `/login`.<br>2. Enter active staff credentials.<br>3. Submit. | JWT session is created and the role-appropriate dashboard opens. | Critical | Tester 1 | Yes |
| AUTH-002 | Authentication | Invalid password | 1. Open staff login.<br>2. Enter a valid username and wrong password.<br>3. Submit. | Login is rejected with a non-sensitive error; no token is stored. | Critical | Tester 2 | Yes |
| AUTH-003 | Authentication | Inactive account | 1. Attempt login with an inactive user.<br>2. Observe response and navigation. | Access is denied and protected routes remain inaccessible. | Critical | Tester 3 | Yes |
| AUTH-004 | Authentication | Account lock after failed attempts | 1. Repeatedly submit a wrong password up to the configured threshold.<br>2. Try the correct password. | Account becomes locked at the configured threshold; correct password is rejected until unlock. | Critical | Tester 1 | Yes |
| AUTH-005 | Authentication | Forced temporary-password change | 1. Login with a password-change-required account.<br>2. Try direct dashboard URL.<br>3. Set a compliant new password. | Only change-password is available until success; old credentials/session version cannot bypass it. | Critical | Tester 2 | Yes |
| AUTH-006 | Public Login | Generate and validate OTP | 1. Open public login.<br>2. Identify a registered visitor.<br>3. Generate OTP.<br>4. Enter the valid OTP. | OTP is accepted once and the correct visitor session/dashboard opens. | Critical | Tester 3 | Yes |
| AUTH-007 | Public Login | Invalid, expired, and reused OTP | 1. Generate OTP.<br>2. Test wrong OTP, expired OTP, then reuse a successful OTP. | Each invalid condition is rejected; attempts do not authenticate the visitor. | Critical | Tester 1 | Yes |
| AUTH-008 | CAPTCHA | CAPTCHA lifecycle | 1. Generate CAPTCHA.<br>2. Submit wrong value.<br>3. Submit correct current value.<br>4. Retry the consumed/expired value. | Only the current correct value validates; consumed/expired challenges fail. | High | Tester 2 | Yes |
| AUTH-009 | Session | Logout and back navigation | 1. Login.<br>2. Logout.<br>3. Use browser Back and direct protected URL. | Token/session is cleared and protected content/API data is not restored. | Critical | Tester 3 | Yes |
| AUTH-010 | Session | Expired or credentials-version-invalid JWT | 1. Use an expired token and a token issued before password change/unlock.<br>2. Call a protected API. | API returns 401 and clients return to login without a redirect loop. | Critical | Tester 1 | Yes |
| DASH-001 | Dashboard | Role-specific tiles and counts | 1. Login as each primary role.<br>2. Open Dashboard.<br>3. Compare tiles to accessible modules and API counts. | Only authorized tiles appear and counts match scoped backend data. | High | Tester 2 | Yes |
| DASH-002 | Dashboard | Dashboard API failure | 1. Simulate summary endpoint failure.<br>2. Open Dashboard.<br>3. Retry. | Loading ends, an actionable error appears, and retry does not duplicate widgets. | High | Tester 3 | Yes |
| NAV-001 | Common UI | Header/sidebar navigation and active state | 1. Login.<br>2. Open every visible sidebar item.<br>3. Use Back/Forward. | Correct route opens, active item tracks route, and no unauthorized menu is exposed. | Medium | Tester 1 | Yes |
| NAV-002 | Common UI | Footer attribution | 1. Visit public and authenticated layouts on desktop/mobile widths.<br>2. Inspect footer. | Required pages show `Design & Development by NITCON LIMITED` legibly without overlap. | Low | Tester 2 | Yes |
| NAV-003 | Common UI | Toast, spinner, dialog, and duplicate click | 1. Trigger a slow successful action and an API failure.<br>2. Double-click Submit.<br>3. Close dialogs. | One request is sent, loading state is visible, measurable toast/error appears, and focus returns correctly. | Medium | Tester 3 | Yes |
| VISREG-001 | Visitor Registration | Register visitor with valid mandatory data | 1. Open registration.<br>2. Complete personal/mobile/address/ID data.<br>3. Capture photo.<br>4. Complete OTP if required.<br>5. Submit. | One visitor is created with identifier, photo/KYC metadata, and success confirmation. | Critical | Tester 1 | Yes |
| VISREG-002 | Visitor Registration | Mandatory and format validation | 1. Leave required fields empty.<br>2. Enter malformed mobile/EPIC and invalid field lengths.<br>3. Submit. | Inline validation identifies each invalid field and no registration API is called. | High | Tester 2 | Yes |
| VISREG-003 | Visitor Registration | Back/previous navigation preserves data | 1. Complete each registration step.<br>2. Move Back then Next.<br>3. Review entered values. | Valid values and captured state are preserved; dependent fields remain consistent. | Medium | Tester 3 | Yes |
| VISREG-004 | Visitor Registration | Duplicate visitor detection | 1. Enter mobile/EPIC belonging to an existing visitor.<br>2. Continue and submit. | Existing registration is identified or duplication is blocked with a clear recovery path. | Critical | Tester 1 | Yes |
| VISREG-005 | Visitor Registration | OTP skip policy | 1. Login as DEO.<br>2. Register using the implemented OTP-skip option.<br>3. Repeat as PUBLIC. | Skip is available only to allowed mode/role; unauthorized skip is rejected by backend. | Critical | Tester 2 | Yes |
| VISREG-006 | Visitor Registration | Prevent duplicate submission | 1. Complete a valid registration.<br>2. Double-click Submit or retry during slow response. | Exactly one visitor is created and UI resolves to a single success state. | Critical | Tester 3 | Yes |
| VISREG-007 | Form Extraction | Extract fields from visitor form | 1. Upload a readable supported form as authorized staff.<br>2. Run extraction.<br>3. Review extracted, ambiguous, unreadable, and not-found fields. | Field statuses are displayed and only confirmed values populate registration. | High | Tester 1 | Yes |
| VISREG-008 | Form Extraction | Unsupported/unreadable form and wrong role | 1. Upload invalid/unreadable content.<br>2. Repeat with PUBLIC.<br>3. Submit extraction. | Validation/service error is measurable; PUBLIC receives 403; no corrupt registration data is saved. | High | Tester 2 | Yes |
| FACE-001 | Face Identification | Camera open, capture, retake, close | 1. Open Identify.<br>2. Allow camera.<br>3. Capture.<br>4. Retake.<br>5. Close/back. | Stream opens once, retake replaces image, and camera tracks stop on close/navigation. | Critical | Tester 3 | Yes |
| FACE-002 | Face Identification | Camera permission denied/unavailable | 1. Deny camera permission or use device without camera.<br>2. Open capture. | Clear recovery guidance/fallback appears and page remains usable. | Critical | Tester 1 | Yes |
| FACE-003 | Face Identification | Single registered face match | 1. Capture a clear registered face.<br>2. Search.<br>3. Open match. | Correct visitor and confidence/match result appear; full history is accessible only to authorized user. | Critical | Tester 2 | Yes |
| FACE-004 | Face Identification | Unknown/no-match face | 1. Capture an unregistered face.<br>2. Search. | No false profile is selected; UI offers the implemented registration/search fallback. | Critical | Tester 3 | Yes |
| FACE-005 | Face Identification | Multiple faces or multiple candidates | 1. Submit image with multiple faces or ambiguous candidates.<br>2. Review response. | System does not silently bind the wrong visitor; candidates/error require explicit resolution. | Critical | Tester 1 | Yes |
| FACE-006 | Face Identification | Poor image and API outage | 1. Submit blurred/dark image.<br>2. Repeat while face API is unavailable.<br>3. Retry. | Quality/service errors are distinct; duplicate searches are prevented and retry works. | High | Tester 2 | Yes |
| FACE-007 | Face Management | Enroll, compare, verify, and restricted delete | 1. Enroll as DEO/admin.<br>2. Compare/verify as permitted staff.<br>3. Attempt delete as DEO then ADMIN. | Operations follow controller roles; deletion is denied to DEO and allowed to ADMIN/SUPER_ADMIN. | Critical | Tester 3 | Yes |
| EPIC-001 | EPIC/KYC | Valid EPIC verification | 1. Enter a valid EPIC during registration/search.<br>2. Verify.<br>3. Compare returned identity with entered visitor. | Verified result and provider/request metadata are shown without silently overwriting conflicting data. | Critical | Tester 1 | Yes |
| EPIC-002 | EPIC/KYC | Invalid and no-match EPIC | 1. Submit malformed EPIC.<br>2. Submit syntactically valid unknown EPIC. | Format error and no-match are distinct; registration proceeds only under implemented fallback policy. | High | Tester 2 | Yes |
| EPIC-003 | EPIC Face | Face-to-EPIC search and combined search | 1. Capture face.<br>2. Run face-to-EPIC search.<br>3. Run combined face+EPIC match. | Returned candidates/match evidence correspond to request; incorrect match requires user resolution. | Critical | Tester 3 | Yes |
| EPIC-004 | EPIC/KYC | Provider timeout/unavailable and retry | 1. Simulate KYC timeout/unavailable response.<br>2. Retry profile KYC endpoint as authorized user. | Pending/failure state is retained, retry is auditable, and registration policy is explicit. | Critical | Tester 1 | Yes |
| WALKIN-001 | Walk-in Counter | Search existing citizen and create walk-in | 1. Login as DEO.<br>2. Open Walk-in Counter.<br>3. Search/select visitor.<br>4. Complete appointment details/document.<br>5. Submit. | One walk-in appointment is created and listed with the code-defined initial status/metadata. | Critical | Tester 2 | Yes |
| WALKIN-002 | Walk-in Counter | Register new visitor then create walk-in | 1. From walk-in, find no citizen.<br>2. Register visitor.<br>3. Return/select visitor.<br>4. Create walk-in. | New visitor is linked to one walk-in without losing context or creating duplicates. | Critical | Tester 3 | Yes |
| WALKIN-003 | Walk-in Counter | Search combinations and empty results | 1. Search by phone, EPIC, name, district, and associate criteria.<br>2. Test partial/duplicate names and no match. | Correct candidates are returned; duplicate names are distinguishable and no-match is explicit. | High | Tester 1 | Yes |
| WALKIN-004 | Walk-in Appointments | List, filter, sort, paginate, and view | 1. Open Walk-in Appointments.<br>2. Apply each filter/search.<br>3. Sort columns.<br>4. Change page.<br>5. View record. | Results, total counts, ordering, page state, photo, and detail are consistent. | High | Tester 2 | Yes |
| WALKIN-005 | Walk-in Workflow | Complete pending walk-in | 1. Open a `PENDING` walk-in as permitted role.<br>2. Perform completion action.<br>3. Refresh lists/history. | State becomes `COMPLETED` once with actor/timestamp and moves to appropriate reporting. | Critical | Tester 3 | Yes |
| WALKIN-006 | Walk-in Security | Unauthorized walk-in access | 1. Login as PUBLIC or unrelated role.<br>2. Enter walk-in URL.<br>3. Call walk-in/DEO API directly. | Route is blocked and API returns 403 without disclosing visitor data. | Critical | Tester 1 | Yes |
| APPT-001 | Appointments | Create normal appointment | 1. Login as permitted user.<br>2. Open New Appointment.<br>3. Select applicant, department, scheme/type, agenda, date/time.<br>4. Upload primary document.<br>5. Submit. | One appointment is created with code-defined initial status, token/application ID and audit metadata. | Critical | Tester 2 | Yes |
| APPT-002 | Appointments | Guest appointment submission | 1. Open Guest Appointment.<br>2. Complete valid public form and attachments.<br>3. Submit once. | Guest appointment endpoint accepts the valid request and returns a traceable confirmation. | Critical | Tester 3 | Yes |
| APPT-003 | Appointments | Appointment mandatory validation | 1. Omit each required applicant/department/agenda/date/document value.<br>2. Submit after each variant. | Specific validation appears and no incomplete appointment is persisted. | High | Tester 1 | Yes |
| APPT-004 | Appointments | Duplicate submission/idempotency | 1. Submit valid appointment under slow network.<br>2. Double-click and retry the same payload. | A single appointment is created or duplicates are explicitly detected. | Critical | Tester 2 | Yes |
| APPT-005 | Appointments | Role-scoped lists | 1. Create records owned by multiple visitors/departments.<br>2. View as PUBLIC, DEO, DEPARTMENT_PA, APPROVER, HCM. | Each list contains only records allowed by owner/department/role scope. | Critical | Tester 3 | Yes |
| APPT-006 | Appointment Detail | View details, history, documents and AI notes | 1. Open an appointment.<br>2. Inspect applicant, metadata, remarks, documents, AI notes, and transitions. | Detail matches source record; missing optional data renders safely. | High | Tester 1 | Yes |
| APPT-007 | Workflow | Submit/DEO process/approver review sequence | 1. Create a record.<br>2. Execute available submit and processing actions in role order.<br>3. Refresh after each. | Only valid transitions occur among `CREATED`, `SUBMITTED`, `DEO_PROCESSED`, `PENDING_APPROVER_REVIEW`/`APPROVER_REVIEW`. | Critical | Tester 2 | Yes |
| APPT-008 | Workflow | Schedule and reschedule | 1. Open eligible appointment.<br>2. Choose available slot and schedule.<br>3. Reschedule to a new valid slot.<br>4. Inspect calendar/history. | State/time becomes `SCHEDULED`, then `RESCHEDULED`; old slot is released and history is retained. | Critical | Tester 3 | Yes |
| APPT-009 | Workflow | Reject eligible appointment | 1. Open eligible record as authorized reviewer.<br>2. Enter rejection reason.<br>3. Confirm. | State is `REJECTED`, reason/actor/time are recorded, and invalid further actions are hidden/rejected. | Critical | Tester 1 | Yes |
| APPT-010 | Workflow | Request missing information and resubmit | 1. Request missing information with remarks.<br>2. Verify `PENDING_REQUEST` or supporting-document state.<br>3. Upload requested document as owner/DEO.<br>4. Refresh. | Request is visible; upload is linked and record returns to the exact implemented review state. | Critical | Tester 2 | Yes |
| APPT-011 | Workflow | Complete and close appointment | 1. Complete an eligible appointment.<br>2. Add follow-up where available.<br>3. Close with remarks.<br>4. Refresh reports. | `COMPLETED` then `CLOSED` are recorded with audit data and correct report placement. | Critical | Tester 3 | Yes |
| APPT-012 | Workflow | Invalid terminal-state transition | 1. Open `CLOSED`, `REJECTED`, or `CANCELLED` record.<br>2. Manipulate request to schedule/approve/return it to pending. | Backend rejects transition; state and audit history remain unchanged. | Critical | Tester 1 | Yes |
| APPT-013 | Public Darbar | Select and schedule for Public Darbar | 1. Select eligible appointment for Public Darbar.<br>2. Create/choose Darbar date.<br>3. Schedule record. | Ordered Public Darbar states and date linkage are persisted and visible. | High | Tester 2 | Yes |
| APPT-014 | CMO/HCM Workflow | CMO review and HCM actions | 1. Move eligible record to CMO/HCM review.<br>2. Test accept, important, modify, snooze, reject as authorized role.<br>3. Inspect action history. | Each action applies only to eligible state, records payload/actor/time, and exposes correct next actions. | Critical | Tester 3 | Yes |
| APPT-015 | Routing | Forward appointment to department/official | 1. Open eligible appointment.<br>2. Choose department/official and remarks.<br>3. Route.<br>4. Login as target department role. | State becomes routed/forwarded, target can see scoped record, and origin/history are retained. | Critical | Tester 1 | Yes |
| DOC-001 | Documents | Upload allowed PDF/JPG/JPEG/PNG | 1. Upload each supported type within configured size as primary/supporting document.<br>2. Save.<br>3. Reopen. | File is stored once with correct name/type/association and can be opened. | High | Tester 2 | Yes |
| DOC-002 | Documents | Reject unsupported, oversized, and empty files | 1. Select executable/unsupported type, oversized file, zero-byte file, and disguised extension.<br>2. Submit. | Client/server reject each invalid file; no physical/orphan metadata is created. | Critical | Tester 3 | Yes |
| DOC-003 | Documents | Authorized download/in-app view | 1. Open document as owner/permitted staff.<br>2. Download and view in Angular and Flutter. | Correct bytes and content type are returned; viewer handles PDF/image and filename safely. | High | Tester 1 | Yes |
| DOC-004 | Documents | Missing physical file | 1. Request metadata whose physical file is unavailable in a controlled test environment.<br>2. View/download. | Measurable not-found error appears without blank/corrupt download or server path disclosure. | High | Tester 2 | Yes |
| DOC-005 | Documents | Unauthorized file/IDOR access | 1. Capture another visitor/department document ID.<br>2. Request it as PUBLIC/unrelated department.<br>3. Change path ID manually. | Access is denied without revealing file content or storage location. | Critical | Tester 3 | Yes |
| REMARK-001 | Remarks | Add role-appropriate remark | 1. Open appointment as APPROVER then HCM.<br>2. Add permitted remark type.<br>3. Save and reopen history. | Remark text, role, username and timestamp are correct and ordered. | High | Tester 1 | Yes |
| REMARK-002 | Remarks | Empty, duplicate, and edit behavior | 1. Submit whitespace remark.<br>2. Double-click valid Save.<br>3. Edit a permitted remark. | Empty is rejected, one valid row is created, and edit follows authorization/audit rules. | High | Tester 2 | Yes |
| REMARK-003 | Remarks | Cross-role remark restriction | 1. Attempt HCM-only remark/action as DEO or PUBLIC using UI and API.<br>2. Attempt restricted Approver action as HCM if not permitted. | UI omits action and backend returns 403; history is unchanged. | Critical | Tester 3 | Yes |
| SCHED-001 | Schedule | Create/update schedule event | 1. Login as authorized staff.<br>2. Create event with valid details.<br>3. Edit it.<br>4. Reload calendar. | Event is created/updated once and displayed at correct date/time. | High | Tester 1 | Yes |
| SCHED-002 | Schedule | Conflict and invalid date | 1. Try conflicting appointment/event assignment.<br>2. Try past/invalid time and missing fields. | Conflict/validation prevents invalid scheduling with clear message. | Critical | Tester 2 | Yes |
| SCHED-003 | Schedule | Assign/remove appointments and delete event | 1. Assign eligible appointments.<br>2. Remove one.<br>3. Delete event with/without assignments. | Relationships and appointment states remain consistent; deletion policy is enforced. | High | Tester 3 | Yes |
| PASS-001 | Appointment Pass | Download eligible walk-in pass | 1. Open eligible walk-in appointment.<br>2. Download pass.<br>3. Inspect identity, appointment and QR. | Valid PDF/pass downloads with correct record data and scannable QR. | Critical | Tester 1 | Yes |
| PASS-002 | Appointment Pass | Download scheduled/rescheduled normal pass | 1. Open `SCHEDULED` and `RESCHEDULED` normal records.<br>2. Download each pass. | Current date/time and a valid QR/token appear; obsolete schedule is not shown. | Critical | Tester 2 | Yes |
| PASS-003 | Appointment Pass | Ineligible status and unauthorized pass | 1. Request pass for ineligible status.<br>2. Request another visitor's pass as PUBLIC. | Download is denied with correct status/authorization response and no token leakage. | Critical | Tester 3 | Yes |
| QR-001 | QR Scanner | Valid QR validation and check-in | 1. Login on mobile as SECURITY/DEO where allowed.<br>2. Scan valid pass.<br>3. Confirm check-in. | Visitor/appointment details match QR and one successful audit/check-in is recorded. | Critical | Tester 1 | Yes |
| QR-002 | QR Scanner | Check-out and movement history | 1. Check in valid visitor.<br>2. Scan/check out as SECURITY.<br>3. Review recent scans/movements as ADMIN. | Ordered check-in/out and actor/time/location audit records are visible to authorized roles. | High | Tester 2 | Yes |
| QR-003 | QR Scanner | Invalid, malformed, expired, duplicate QR | 1. Scan random/malformed QR, expired/revoked QR, then repeat valid check-in.<br>2. Observe each response. | Each condition is distinguished; duplicate movement is blocked or explicitly recorded per rule. | Critical | Tester 3 | Yes |
| QR-004 | QR Scanner | Permission denied and camera close | 1. Deny camera permission.<br>2. Retry/allow.<br>3. Close and navigate back. | Guidance appears, retry succeeds, and camera resource is released. | High | Tester 1 | Yes |
| QR-005 | QR Security | Scanner role matrix | 1. Validate as DEO.<br>2. Attempt check-in/out as DEO.<br>3. Repeat as SECURITY and query audits as SECURITY/ADMIN. | `/qr/validate` and privileged movement/audit endpoints enforce their distinct role policies. | Critical | Tester 2 | Yes |
| REPORT-001 | Reports | Completed/rejected/closed report loading | 1. Open each report as an allowed role.<br>2. Compare records and totals to known statuses. | Only correct status records and scoped departments are shown. | High | Tester 3 | Yes |
| REPORT-002 | Reports | Search/date/status filters | 1. Apply search, boundary dates, status and department filters individually and combined.<br>2. Clear filters. | Results/totals match all criteria; clear restores default dataset. | High | Tester 1 | Yes |
| REPORT-003 | Reports | Sorting and pagination | 1. Sort supported columns ascending/descending.<br>2. Change page and size.<br>3. Return to prior page. | Stable server ordering, page counts and filter state are preserved. | Medium | Tester 2 | Yes |
| REPORT-004 | Reports | Empty and API-error states | 1. Apply no-result criteria.<br>2. Simulate 4xx/5xx/timeout.<br>3. Retry. | Empty state differs from error; spinner ends and retry does not duplicate rows. | High | Tester 3 | Yes |
| REPORT-005 | Reports | PDF/Excel export | 1. Apply filters.<br>2. Export PDF and Excel where exposed.<br>3. Open files and compare rows. | Files open successfully and contain the filtered/scoped dataset with correct headings. | High | Tester 1 | Yes |
| REPORT-006 | Heatmap | Heatmap aggregation and access | 1. Open Heatmap as authorized role.<br>2. Change available criteria.<br>3. Attempt as unauthorized role. | Aggregation and labels match report data; unauthorized access is blocked. | High | Tester 2 | Yes |
| AUDIT-001 | Audit Trail | Filtered audit retrieval | 1. Generate login, create and status actions.<br>2. Open Audit Trail.<br>3. Filter by actor/action/date. | Immutable-looking audit rows show correct actor, event, entity and timestamp under role scope. | High | Tester 3 | Yes |
| AUDIT-002 | Audit Trail | Department audit isolation | 1. Create activity in two departments.<br>2. Login as DEPARTMENT_ADMIN/PA of one department.<br>3. Query UI and API. | Only own-department logs are returned; SUPER_ADMIN can retrieve global scope. | Critical | Tester 1 | Yes |
| LEGACY-001 | Legacy Import | Upload valid multi-sheet workbook | 1. Login as ADMIN/DEO.<br>2. Upload valid multi-sheet workbook.<br>3. Inspect detected sheets, columns and preview. | Batch is created, all sheets/columns are detected, and preview matches source rows. | Critical | Tester 2 | Yes |
| LEGACY-002 | Legacy Import | Map, validate and execute import | 1. Apply mapping to each sheet.<br>2. Validate batch.<br>3. Execute.<br>4. Review summary. | Valid mapping reaches ready state and imports with accurate counts/statuses. | Critical | Tester 3 | Yes |
| LEGACY-003 | Legacy Import | Partial failure, retry, skip and exports | 1. Import workbook with valid/invalid rows.<br>2. Review errors.<br>3. Export error/summary CSV.<br>4. Retry or skip failed sheet. | Counts reconcile; error rows are actionable; retry/skip updates state without duplicating successes. | High | Tester 1 | Yes |
| LEGACY-004 | Legacy Import | Invalid/large/duplicate workbook | 1. Upload invalid Excel, missing-column workbook, duplicate workbook and representative large workbook (~36 MB if within configured limit). | Each follows configured size/type/duplicate policy without crash or partial orphan batch. | Critical | Tester 2 | Yes |
| LEGACY-005 | Legacy Import | Import ownership and admin separation | 1. Upload as DEO A.<br>2. Request batch as DEO B.<br>3. List as ADMIN.<br>4. Try dataset creation as DEO. | DEO sees own permitted batches; ADMIN sees administrative scope; dataset creation is ADMIN-only. | Critical | Tester 3 | Yes |
| LEGACY-006 | Legacy Search | Search field combinations | 1. Search by EPIC, name, mobile, village and address separately/combined.<br>2. Test partial/duplicate/no-match. | Matching legacy people are ranked/displayed without merging primary visitor records. | High | Tester 1 | Yes |
| AI-001 | AI/OCR | Analyze readable PDF and image | 1. Upload readable PDF then JPG/PNG as authorized staff.<br>2. Run analysis.<br>3. Compare summary/important details to source. | OCR/analysis returns traceable, non-empty results without altering original document. | High | Tester 2 | Yes |
| AI-002 | AI/OCR | Scanned/unreadable/empty document | 1. Analyze scanned readable image, blurred image and empty file.<br>2. Review result status. | Partial/unreadable/validation outcomes are explicit; fabricated details are not presented as facts. | Critical | Tester 3 | Yes |
| AI-003 | AI Notes | Load and regenerate document AI notes | 1. Open appointment AI notes.<br>2. Regenerate as allowed role.<br>3. Refresh and inspect status/time. | One regeneration is tracked and updated notes/status appear against correct document. | High | Tester 1 | Yes |
| AI-004 | AI | Duplicate and priority suggestions | 1. Submit duplicate-check inputs and priority insight request.<br>2. Compare rationale to record.<br>3. Attempt as unauthorized role. | Advisory response has supported rationale and never bypasses workflow; unauthorized call is rejected. | High | Tester 2 | Yes |
| AI-005 | AI | Model unavailable/timeout/empty response | 1. Simulate unavailable, timeout, partial and empty model responses.<br>2. Retry. | UI distinguishes conditions, preserves user data, and core appointment workflow remains available. | Critical | Tester 3 | Yes |
| SCHEME-001 | Schemes | List and apply to scheme | 1. Open Schemes as permitted role.<br>2. Select scheme.<br>3. Submit valid JSON/multipart application.<br>4. View visitor applications. | Application is created once, documents link correctly, and visitor/staff scope is enforced. | High | Tester 1 | Yes |
| SCHEME-002 | Schemes | Scheme application validation/status | 1. Submit missing/invalid data.<br>2. Update status as authorized reviewer.<br>3. Attempt status update as PUBLIC. | Validation blocks bad input; staff update is recorded; PUBLIC receives 403. | High | Tester 2 | Yes |
| ADMIN-001 | User Management | Create department-scoped user | 1. Login as permitted administrator.<br>2. Enter unique username, role, department and temporary password.<br>3. Save. | User is created with correct scope, active state and password-change requirement. | Critical | Tester 3 | Yes |
| ADMIN-002 | User Management | Duplicate/invalid user | 1. Submit duplicate username, invalid role/department and weak password.<br>2. Save each. | Specific validation rejects each case and no duplicate user is created. | High | Tester 1 | Yes |
| ADMIN-003 | User Management | Activate, deactivate, unlock and delete | 1. Perform each action on eligible user.<br>2. Test login after each.<br>3. Attempt protected SUPER_ADMIN deletion as non-super role. | States affect login immediately; audit is recorded; protected operations are denied. | Critical | Tester 2 | Yes |
| ADMIN-004 | User Management | Department-admin tenant isolation | 1. Login as DEPARTMENT_ADMIN.<br>2. List/create/update users in own department.<br>3. Manipulate ID to another department or SUPER_ADMIN role. | Own permitted roles are manageable; cross-tenant/elevated operations return 403. | Critical | Tester 3 | Yes |
| ADMIN-005 | Departments | Create/update/activate/deactivate department | 1. Login as SUPER_ADMIN.<br>2. Create department.<br>3. Edit then deactivate/reactivate.<br>4. Test affected user login. | State and metadata update correctly; inactive department blocks non-super users as implemented. | Critical | Tester 1 | Yes |
| ADMIN-006 | Department Requests | Submit, approve and reject request | 1. Submit public department access request.<br>2. Review as SUPER_ADMIN.<br>3. Approve one and reject another with reason. | State is correct; approval creates/links scoped access once; rejection reason is retained. | Critical | Tester 2 | Yes |
| ADMIN-007 | Configuration | Manage schemes and appointment types | 1. Open admin scheme/type pages as allowed role.<br>2. Create/update/activate/deactivate entries.<br>3. Verify forms/lists. | Active configuration becomes selectable; inactive entries are handled consistently without corrupting history. | High | Tester 3 | Yes |
| GRV-001 | Grievances | Public grievance creation and viewing | 1. Login as PUBLIC.<br>2. Submit valid grievance.<br>3. Open own list/detail. | Grievance is created with initial status and only owner-accessible data is shown. | High | Tester 1 | Yes |
| GRV-002 | Grievances | Staff status/response workflow | 1. Open grievance as staff.<br>2. Perform available status/response actions.<br>3. Refresh as citizen. | Authorized changes, response, actor and timestamp appear consistently. | High | Tester 2 | Yes |
| DIR-001 | Directions | Direction and follow-up lifecycle | 1. Create/view direction as authorized HCM/approver.<br>2. Open as assigned department role.<br>3. Move follow-up PENDING → IN_PROGRESS → COMPLETED. | Assignment and each valid status transition are scoped and audited. | High | Tester 3 | Yes |
| DIR-002 | Directions | Unauthorized/cross-department follow-up | 1. Attempt direction creation as DEO.<br>2. Attempt another department's follow-up update as DEPARTMENT_PA. | Backend returns 403 and target record remains unchanged. | Critical | Tester 1 | Yes |
| PHOTO-001 | Photos | Authenticated photo across contexts | 1. View same visitor in profile, appointment, walk-in and reports on web/mobile.<br>2. Compare thumbnail/detail. | Authorized image resolves consistently with correct aspect/fallback and no token in URL. | High | Tester 2 | Yes |
| PHOTO-002 | Photos | Missing/invalid/unauthorized photo | 1. Test null URL, broken URL and another visitor's protected photo request.<br>2. Observe pages. | Safe placeholder appears; layout remains stable; unauthorized bytes are not returned. | Critical | Tester 3 | Yes |
| MOBILE-001 | Mobile | Secure login persistence and logout | 1. Login on mobile.<br>2. Restart app.<br>3. Logout and restart.<br>4. Inspect protected navigation. | Valid session restores securely; logout clears secure state and protected screens cannot reopen. | Critical | Tester 1 | Yes |
| MOBILE-002 | Mobile Offline | Queue offline appointment/action | 1. Disconnect network.<br>2. Perform an explicitly offline-enabled action.<br>3. Open Pending Sync.<br>4. Reconnect. | Action is queued once, visibly pending, then syncs with server without duplication. | Critical | Tester 2 | Yes |
| MOBILE-003 | Mobile Offline | Sync conflict and partial failure | 1. Queue multiple records offline.<br>2. Change one server-side before reconnect.<br>3. Sync. | Successes complete; conflict/failure remains actionable and retry does not replay successes. | Critical | Tester 3 | Yes |
| MOBILE-004 | Mobile | Connectivity transitions and stale data | 1. Open cached screen online.<br>2. Go offline/online repeatedly.<br>3. Refresh. | Connectivity state is accurate; cached data is labelled/handled and final refresh reconciles server state. | High | Tester 1 | Yes |
| MOBILE-005 | Mobile | Document viewer lifecycle | 1. Open PDF/image.<br>2. Background/resume app.<br>3. Back out during load/error. | Viewer renders supported content, handles lifecycle/error, and releases temporary resources. | Medium | Tester 2 | Yes |
| SEC-001 | Security | Unauthenticated API matrix | 1. Call representative appointment, user, report, photo, file, face and QR endpoints without JWT.<br>2. Record status/body. | Protected endpoints return 401 with no PII or stack trace. | Critical | Tester 3 | Yes |
| SEC-002 | Security | Wrong-role API matrix | 1. Authenticate as each low-privilege role.<br>2. Call representative higher-privilege endpoints directly. | Each unauthorized call returns 403 and causes no state change. | Critical | Tester 1 | Yes |
| SEC-003 | Security | IDOR appointment/visitor/grievance/scheme | 1. Replace owned entity ID with another visitor/department ID.<br>2. GET and mutate it. | Ownership/tenant checks deny access without confirming sensitive entity existence. | Critical | Tester 2 | Yes |
| SEC-004 | Security | Input validation and safe errors | 1. Submit boundary-length, HTML/script-like, SQL-like and malformed JSON inputs to forms/APIs non-destructively.<br>2. Reopen records. | Inputs are validated/encoded; no script executes and errors expose no internals. | Critical | Tester 3 | Yes |
| SEC-005 | Security | Direct Angular route access | 1. Enter each guarded URL as unauthenticated and unauthorized roles.<br>2. Try browser refresh. | Guards redirect/block consistently; backend independently enforces authorization. | Critical | Tester 1 | Yes |

## Regression risks / observations

| Module | File/API | Observation | Severity | Recommended Follow-up |
|---|---|---|---|---|
| Roles | Backend `User.UserRole`, mobile `UserRole`, controller annotations | Scanner naming differs (`SECURITY` vs mobile `SECURITY_POLICE`), while user API describes `ROLE_SECURITY`. | High | Confirm canonical JWT/database role and add contract test across clients. |
| Roles | Appointment controllers/services and migration V14 | `APPROVER_JT_SECY` remains in authorization code although migration maps it to `APPROVER`. | Medium | Confirm compatibility necessity and remove/document stale references in a separate change. |
| Roles | Controller annotations | `CITIZEN` is accepted by some APIs while normal visitor JWT role is `PUBLIC`. | Medium | Confirm whether legacy tokens still exist and document canonical public role. |
| Status model | `Appointment.AppointmentStatus` | 28 persisted states coexist with two-state `WalkInStatus`; UI labels/actions may not expose all states. | High | Product-owner transition review and automated transition-table tests. |
| Mobile parity | Mobile navigation vs Angular routes | No dedicated mobile legacy-import or department-management screen; QR/offline sync are mobile-only. | Medium | Confirm intentional platform scope. |
| Authorization | Route feature guards vs backend role annotations | Frontend feature visibility and backend role sets are not expressed from one shared policy. | High | Maintain an executable role-contract matrix to detect drift. |
| Reports | Executive vs department report controllers | Executive reports exclude department roles while standard reports include department-scoped roles. | Medium | Confirm expected distinction and label UI accordingly. |
| Legacy import | Separate legacy controllers/data model | Separation from primary data is architectural and cannot be proven by UI-only testing. | High | Add environment-level database connection/schema verification to test evidence. |
| Dead/reachability | AI chatbot, AI insights, appointment detail/CMO modal | Components exist but are not direct top-level Angular routes; reachability depends on embedding/action paths. | Medium | Confirm each is reachable in built UI and document any intentionally dormant component. |

## Coverage/exclusion notes

- Every Angular top-level route is represented by a module case or the common navigation/security cases. Embedded components are covered in their parent workflow or listed as reachability risks.
- Every Flutter screen is covered directly or through its matching functional module; Main Shell/DEO Home are covered by navigation and role dashboard cases.
- Every backend controller is mapped above. `ReferenceDataController` is exercised indirectly by forms/configuration; `AppointmentTypeController` by configuration and appointment creation; `FileUploadController` by document cases; internal health/monitoring endpoints are outside user-facing regression scope.
- All persisted appointment states require test-data fixtures. Cases APPT-007 through APPT-015 cover state families and invalid terminal transitions; a product-approved transition table is still required to enumerate every legal edge among all 28 states without inventing behavior.
- No application source, migration, configuration, or database file was modified while producing this catalog.
