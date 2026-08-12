# Executive Appointment Reporting QA Test Cases

Actual Result and Pass/Fail are intentionally left for execution evidence during UAT.

| Test Case ID | Module | Scenario | Preconditions | Role | Steps | Input | Expected Result | Actual Result | Pass/Fail | Severity | Remarks |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-COMP-001 | Completed | Menu visibility | Logged in | APPROVER | Open Reports menu | — | Completed Appointments visible | — | — | High | RBAC |
| TC-COMP-002 | Completed | Menu visibility | Logged in | HCM | Open Reports menu | — | Completed Appointments visible | — | — | High | RBAC |
| TC-COMP-003 | Completed | Unauthorized access | Logged in | DEO | Open route and API | URL/API | Route blocked and API 403 | — | — | Critical | Security |
| TC-COMP-004 | Completed | Scheduled records | Completed scheduled exists | APPROVER | Open list | HCM_MET_COMPLETED | Record is returned | — | — | High | — |
| TC-COMP-005 | Completed | Walk-in records | Completed walk-in exists | APPROVER | Open list | WALK_IN/COMPLETED | Record is returned | — | — | High | — |
| TC-COMP-006 | Completed | Exclude pending | Pending exists | HCM | Open list | PENDING | Record excluded | — | — | High | — |
| TC-COMP-007 | Completed | Exclude scheduled active | Scheduled active exists | HCM | Open list | SCHEDULED | Record excluded | — | — | High | — |
| TC-COMP-008 | Completed | Exclude rejected | Rejected exists | HCM | Open list | REJECTED | Record excluded | — | — | High | — |
| TC-COMP-009 | Completed | Pagination | More than page size | APPROVER | Use Next/Previous | page,size | Correct server page returned | — | — | Medium | — |
| TC-COMP-010 | Completed | Sorting | Multiple records | APPROVER | Request allowed sort | completedAt,desc | Correct order returned | — | — | Medium | Sort allowlist |
| TC-COMP-FLT-001 | Filters | Department | Matching data | APPROVER | Apply filter | Department name | Only matching records | — | — | High | Server-side |
| TC-COMP-FLT-002 | Filters | Scheme | Matching data | APPROVER | Apply filter | Scheme | Only matching records | — | — | High | Server-side |
| TC-COMP-FLT-003 | Filters | Constituency | Matching data | APPROVER | Apply filter | Constituency | Only matching records | — | — | Medium | Server-side |
| TC-COMP-FLT-004 | Filters | District | Matching data | APPROVER | Apply filter | District | Only matching records | — | — | Medium | Server-side |
| TC-COMP-FLT-005 | Filters | Completed date range | Matching data | APPROVER | Apply dates | From/to | Uses completedAt range | — | — | High | — |
| TC-COMP-FLT-006 | Filters | Status | Both completion statuses | HCM | Select status | COMPLETED | Exact allowed completion status | — | — | Medium | — |
| TC-COMP-FLT-007 | Filters | Follow-up | Follow-ups exist | HCM | Select status | OVERDUE | Computed overdue matches | — | — | High | — |
| TC-COMP-FLT-008 | Filters | MLA | Referred name exists | HCM | Enter MLA | Name fragment | Matching records | — | — | Medium | — |
| TC-COMP-FLT-009 | Filters | Agenda type | Agenda exists | HCM | Enter agenda | Agenda | Matching records | — | — | Medium | — |
| TC-COMP-FLT-010 | Filters | Combined filters | Matching data | APPROVER | Apply all | Dept+district+follow-up+dates | Intersection returned | — | — | High | — |
| TC-COMP-DTL-001 | Detail | Applicant fields | Record exists | APPROVER | View | ID | Correct applicant data | — | — | High | — |
| TC-COMP-DTL-002 | Detail | Citizen photo | Stored photo exists | APPROVER | View | ID | Authorized thumbnail displayed | — | — | High | No path exposed |
| TC-COMP-DTL-003 | Detail | Appointment fields | Record exists | APPROVER | View | ID | Dates/type/source shown | — | — | High | — |
| TC-COMP-DTL-004 | Detail | Petition | Petition exists | HCM | View | ID | Deterministic summary shown | — | — | High | — |
| TC-COMP-DTL-005 | Detail | Directions | Directions exist | HCM | View | ID | Structured chronological directions | — | — | High | — |
| TC-COMP-DTL-006 | Detail | Assigned department | Direction exists | HCM | View | ID | Assigned department shown | — | — | High | — |
| TC-COMP-DTL-007 | Detail | Follow-up | Follow-up exists | HCM | View | ID | Status/officer/due date shown | — | — | High | — |
| TC-COMP-DTL-008 | Detail | Multiple actions | Multiple actions exist | HCM | View | ID | All action items shown | — | — | High | — |
| TC-COMP-DTL-009 | Detail | Documents | Documents exist | APPROVER | View | ID | Metadata only; no path | — | — | Critical | Security |
| TC-COMP-DTL-010 | Detail | AI summary | Provider available | HCM | View | ID | Grounded AI briefing shown | — | — | Medium | Labelled assisted |
| TC-COMP-DTL-011 | Detail | AI unavailable | Provider unavailable | HCM | View | ID | Fallback shown; detail succeeds | — | — | High | Resilience |
| TC-PDF-001 | PDF | Generate | Completed exists | APPROVER | Click PDF | ID | PDF downloads | — | — | High | — |
| TC-PDF-002 | PDF | Applicant data | PDF generated | APPROVER | Inspect | PDF | Applicant section present | — | — | High | — |
| TC-PDF-003 | PDF | Appointment data | PDF generated | APPROVER | Inspect | PDF | Appointment section present | — | — | High | — |
| TC-PDF-004 | PDF | Petition | PDF generated | APPROVER | Inspect | PDF | Petition present | — | — | Medium | — |
| TC-PDF-005 | PDF | Directions | PDF generated | HCM | Inspect | PDF | Directions present | — | — | High | — |
| TC-PDF-006 | PDF | Assigned department | PDF generated | HCM | Inspect | PDF | Department present | — | — | High | — |
| TC-PDF-007 | PDF | Follow-up | PDF generated | HCM | Inspect | PDF | Follow-up status present | — | — | High | — |
| TC-PDF-008 | PDF | Documents | PDF generated | HCM | Inspect | PDF | Document metadata present | — | — | Medium | — |
| TC-PDF-009 | PDF | Photo | Photo exists | HCM | Export | ID | Stored citizen photo embedded | — | — | High | No provider call |
| TC-PDF-010 | PDF | AI summary | Detail exists | HCM | Export | ID | Labelled AI summary present | — | — | Medium | — |
| TC-PDF-011 | PDF | Missing photo | No photo | HCM | Export | ID | PDF succeeds with placeholder | — | — | High | — |
| TC-PDF-012 | PDF | Unauthorized | Logged in | DEO | Call endpoint | ID | 403 | — | — | Critical | — |
| TC-XLS-001 | Excel | Export | Completed data | APPROVER | Export | Filters | XLSX downloads | — | — | High | — |
| TC-XLS-002 | Excel | Department filter | Data exists | APPROVER | Export filtered | Department | Export matches filter | — | — | High | — |
| TC-XLS-003 | Excel | Scheme filter | Data exists | APPROVER | Export filtered | Scheme | Export matches filter | — | — | High | — |
| TC-XLS-004 | Excel | District filter | Data exists | APPROVER | Export filtered | District | Export matches filter | — | — | Medium | — |
| TC-XLS-005 | Excel | Follow-up filter | Data exists | APPROVER | Export filtered | OVERDUE | Export matches filter | — | — | High | — |
| TC-XLS-006 | Excel | Completed sheet | Export generated | APPROVER | Inspect | Sheet 1 | Required columns/rows | — | — | High | — |
| TC-XLS-007 | Excel | Directions sheet | Export generated | APPROVER | Inspect | Sheet 2 | Direction/action rows | — | — | High | — |
| TC-XLS-008 | Excel | Documents sheet | Export generated | APPROVER | Inspect | Sheet 3 | Metadata rows | — | — | Medium | — |
| TC-XLS-009 | Excel | Sensitive exclusion | Export generated | APPROVER | Inspect | Workbook | No photo/Base64/biometrics | — | — | Critical | — |
| TC-REJ-001 | Rejected | Menu visibility | Logged in | APPROVER | Open Reports | — | Menu visible | — | — | High | — |
| TC-REJ-002 | Rejected | Menu visibility | Logged in | HCM | Open Reports | — | Menu visible | — | — | High | — |
| TC-REJ-003 | Rejected | List | Rejected exists | APPROVER | Open list | — | Data loads | — | — | High | — |
| TC-REJ-004 | Rejected | Appointment ID search | Data exists | APPROVER | Filter | Application ID | Match returned | — | — | Medium | — |
| TC-REJ-005 | Rejected | Applicant search | Data exists | APPROVER | Filter | Name fragment | Match returned | — | — | Medium | — |
| TC-REJ-006 | Rejected | EPIC search | Data exists | APPROVER | Filter | EPIC | Match returned | — | — | High | — |
| TC-REJ-007 | Rejected | Department search | Data exists | APPROVER | Filter | Department | Match returned | — | — | Medium | — |
| TC-REJ-008 | Rejected | Reason search | Data exists | HCM | Filter | Reason fragment | Match returned | — | — | High | — |
| TC-REJ-009 | Rejected | Rejected date | Data exists | HCM | Apply dates | From/to | Uses rejectedAt | — | — | High | — |
| TC-REJ-010 | Rejected | Detail reason | Data exists | HCM | View | ID | Reason shown | — | — | High | — |
| TC-REJ-011 | Rejected | Rejected by | Data exists | HCM | View | ID | Actor shown | — | — | High | — |
| TC-REJ-012 | Rejected | Rejected date | Data exists | HCM | View | ID | Timestamp shown | — | — | High | — |
| TC-REJ-013 | Rejected | Status history | Audits exist | HCM | View | ID | Ordered transitions shown | — | — | High | — |
| TC-REJ-014 | Rejected | Read-only | Detail open | HCM | Inspect/actions | — | No mutation controls/API | — | — | Critical | — |
| TC-RPT-SEC-001 | Security | Unauthenticated | No login | — | Call API | URL | 401 | — | — | Critical | — |
| TC-RPT-SEC-002 | Security | Unauthorized role | Logged in | DEO | Call API | URL | 403 | — | — | Critical | — |
| TC-RPT-SEC-003 | Security | ID manipulation | Authorized user | APPROVER | Change detail ID | Other/nonmatching ID | Only valid module record or 404 | — | — | Critical | IDOR |
| TC-RPT-SEC-004 | Security | PDF IDOR | Authorized user | APPROVER | Change PDF ID | Other/noncompleted ID | 404/denied | — | — | Critical | — |
| TC-RPT-SEC-005 | Security | Excel authorization | Logged in | DEO | Export | Filters | 403 | — | — | Critical | — |
| TC-RPT-SEC-006 | Security | Frontend tampering | Logged in | DEO | Force menu/route | DevTools | Backend remains 403 | — | — | Critical | — |
| TC-RPT-PERF-001 | Performance | 1000+ records | Seeded data | APPROVER | Paginate | size=20 | Bounded page response | — | — | High | — |
| TC-RPT-PERF-002 | Performance | Server filters | SQL logging | APPROVER | Apply filter | Combined | Criteria executes server-side | — | — | High | — |
| TC-RPT-PERF-003 | Performance | N+1 | SQL metrics enabled | APPROVER | Load list | Page | Bounded batch enrichment queries | — | — | High | — |
| TC-RPT-PERF-004 | Performance | No external photo lookup | Provider logs enabled | HCM | Generate PDF | ID | No EPIC/Face API call | — | — | Critical | — |
| TC-RPT-PERF-005 | Performance | Large Excel | Large result | APPROVER | Export | Filters | Limit enforced; no photo/raw files | — | — | High | Memory monitor |
