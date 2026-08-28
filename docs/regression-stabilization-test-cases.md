# MeghaConnect Stabilization Regression Cases

All cases are active. Default assignment is `QA Team` unless noted.

| Test Case ID | Module | Scenario / Test Case | Test Steps | Expected Result | Priority | Default Assigned To | Active |
|---|---|---|---|---|---|---|---|
| REG-FOOT-001 | Common UI | Angular Home footer | Open Home; scroll to bottom. | `Design & Development by NITCON LIMITED` is visible. | Medium | UI QA | Yes |
| REG-FOOT-002 | Common UI | DEO footer | Login as DEO; open Dashboard and an operational page; scroll. | Footer is visible without overlap. | Medium | UI QA | Yes |
| REG-FOOT-003 | Common UI | APPROVER footer | Login as APPROVER; inspect Dashboard and Appointments. | Footer is visible without overlap. | Medium | UI QA | Yes |
| REG-FOOT-004 | Common UI | HCM footer | Login as HCM; inspect Dashboard and Appointments. | Footer is visible without overlap. | Medium | UI QA | Yes |
| REG-FOOT-005 | Mobile | Mobile footer | Open staff Home/Dashboard and an operational screen. | Footer is visible above the safe bottom edge. | Medium | Mobile QA | Yes |
| REG-BACK-001 | Angular navigation | Walk-in back | Login as DEO; open Walk-in Counter; press back arrow. | DEO Dashboard opens explicitly. | High | UI QA | Yes |
| REG-BACK-002 | Angular navigation | Register Visitor back | Open Register Visitor; press Back to Dashboard. | Logged-in role Dashboard opens. | High | UI QA | Yes |
| REG-BACK-003 | Angular navigation | Public Identification back | Open Public Identification; press Back to Dashboard. | Logged-in role Dashboard opens. | High | UI QA | Yes |
| REG-BACK-004 | Angular navigation | Appointments back | Open Appointments; press Back to Dashboard. | Logged-in role Dashboard opens. | High | UI QA | Yes |
| REG-BACK-005 | Mobile navigation | Operational page back | Open an operational page; use its dashboard/home action. | Mobile role Dashboard opens without duplicate stack entries. | High | Mobile QA | Yes |
| REG-QA-001 | DEO Dashboard | New Appointment action | Login as DEO; select Quick Actions > New Appointment. | Current citizen-identification/new-appointment flow opens. | High | UI QA | Yes |
| REG-QA-002 | DEO Dashboard | New Appointment styling | Compare New Appointment and Walk-in Counter actions. | Both use the same success/green treatment. | Medium | UI QA | Yes |
| REG-REJVIEW-001 | Rejected Appointments | Readable detail | Open Rejected Appointments; select View. | White detail panel opens and all labels/values have readable contrast. | High | UI QA | Yes |
| REG-REJVIEW-002 | Rejected Appointments | Rejection metadata | View a rejected record with metadata. | Rejected date, actor, and reason are visible. | High | UI QA | Yes |
| REG-REJVIEW-003 | Mobile | Rejected detail contrast | Open a rejected appointment detail. | Appointment and rejection values are readable and consistently styled. | High | Mobile QA | Yes |
| REG-HCM-001 | HCM navigation | Deprecated menu removed | Login as HCM; inspect navigation. | No HCM Actions menu is present. | High | UI QA | Yes |
| REG-HCM-002 | HCM authorization | Valid access retained | Open All, Walk-in, Completed, Rejected, and Closed appointments as HCM. | Authorized modules remain accessible. | High | Security QA | Yes |
| REG-HCM-003 | HCM routing | Deprecated URL | Navigate directly to `/hcm/appointments`. | User is safely redirected to unified Dashboard. | High | Security QA | Yes |
| REG-WALKFILTER-001 | Walk-in API | Pending included | Query active walk-ins with a PENDING walk-in present. | PENDING walk-in appears. | Blocker | API QA | Yes |
| REG-WALKFILTER-002 | Walk-in API | Pending request included | Query active walk-ins with PENDING_REQUEST present. | PENDING_REQUEST walk-in appears. | Blocker | API QA | Yes |
| REG-WALKFILTER-003 | Walk-in API | Rejected excluded | Query active walk-ins with a REJECTED walk-in present. | REJECTED row is absent. | Blocker | API QA | Yes |
| REG-WALKFILTER-004 | Walk-in API | Completed excluded | Query active walk-ins with a COMPLETED walk-in present. | COMPLETED row is absent. | Blocker | API QA | Yes |
| REG-WALKFILTER-005 | Walk-in API | HCM accepted excluded | Query active walk-ins with HCM_ACCEPTED present. | HCM_ACCEPTED row is absent. | Blocker | API QA | Yes |
| REG-WALKFILTER-006 | Walk-in API | Direct response validation | GET `/api/v1/appointments?page=0&size=100&status=PENDING,PENDING_REQUEST&appointmentType=WALKIN&sort=createdAt,desc`. | Every row has source WALKIN and status PENDING or PENDING_REQUEST; paging/sort remain valid. | Blocker | API QA | Yes |
| REG-WALKFILTER-007 | Mobile | Corrected API consumed | Open mobile Walk-in Appointments. | Only server-returned active walk-ins appear. | High | Mobile QA | Yes |
| REG-SOURCE-001 | Appointment grid | Type/source mapping | Load a row with type `B2 Walk-in` and source `WALKIN`. | Appointment Type shows `B2 Walk-in`; Appointment Source shows `WALKIN`. | High | UI QA | Yes |
| REG-SOURCE-002 | Appointment grid | Source sort/display | Sort the Appointment Source column. | Display and sorting use `appointmentSource`, not event type. | Medium | UI QA | Yes |
| REG-REJECT-001 | Rejection workflow | Metadata persistence | Reject a PENDING appointment with a reason; GET it again and query DB. | Status REJECTED; rejectedAt non-null; rejectedBy is authenticated actor; reason matches. | Blocker | API QA | Yes |
| REG-REJECT-002 | Rejection workflow | Approver identity | Reject as APPROVER. | rejectedBy equals the authenticated APPROVER username. | High | Security QA | Yes |
| REG-REJECT-003 | Rejection workflow | HCM identity | Reject as permitted HCM user. | rejectedBy equals authenticated HCM username. | High | Security QA | Yes |
| REG-REJECT-004 | Rejected UI | Persisted values displayed | Refresh Rejected list and View the record. | Persisted rejectedAt, rejectedBy, and reason appear. | High | UI QA | Yes |
| REG-REJECT-005 | Audit | Rejection history | Inspect appointment history/audit after rejection. | Old status to REJECTED, reason, actor, role, and timestamp are recorded. | Blocker | Audit QA | Yes |
| REG-REJECT-006 | Validation | Blank reason rejected | Submit rejection with blank reason. | Validation error; status and metadata remain unchanged. | High | API QA | Yes |
| API-REG-001 | Backend API | Combined walk-in predicates | Call the active walk-in GET endpoint directly. | Query applies source WALKIN AND requested status set. | Blocker | API QA | Yes |
| API-REG-002 | Backend API | Reject then read | Reject through workflow endpoint; GET same appointment. | Response contains persisted rejection metadata. | Blocker | API QA | Yes |
| REG-SEC-001 | Security | Existing controls | Run authentication/authorization suite and negative role calls. | Spring Security, JWT, OTP, CAPTCHA, Redis, face, EPIC, OCR/AI and audit controls are unchanged. | Blocker | Security QA | Yes |
