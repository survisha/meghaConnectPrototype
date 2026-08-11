# MeghaConnect Functional Revision QA Matrix

This matrix is the release acceptance checklist for the canonical appointment, walk-in, direction follow-up, decision-support, reporting, and role revisions. Automated cases are executed by the repository test/build commands; role-specific end-to-end cases require seeded QA users and a deployed database.

| ID | Area | Scenario | Expected result | Evidence |
|---|---|---|---|---|
| ROLE-01 | Roles | Existing `DATA_ENTRY_OPERATOR` user migrates | Role is `DEO`; history is retained | Flyway V70 |
| ROLE-02 | Roles | Existing OSD/CMO/CMO_OFFICER user migrates | Role is `APPROVER`; no obsolete active role remains | Flyway V71 |
| ROLE-03 | Roles | CMO department admin opens user creation | Creatable roles are APPROVER, DEO and HCM | Angular role policy/build |
| ROLE-04 | Security | APPROVER accesses an HCM/Approver-shared endpoint | Access allowed | Spring security tests/manual API |
| ROLE-05 | Security | HCM accesses an HCM/Approver-shared endpoint | Access allowed | Spring security tests/manual API |
| APT-01 | Scheduled | Create a scheduled appointment | Initial state is PENDING and category SCHEDULED | Unit/API test |
| APT-02 | Scheduled | PENDING appointment is scheduled | State becomes SCHEDULED with date/time | Unit/API test |
| APT-03 | Scheduled | PENDING appointment is rejected | State becomes REJECTED and reason is retained | Unit/API test |
| APT-04 | Scheduled | PENDING appointment is routed | State becomes ROUTED_TO_OFFICIAL; routing target retained | API test |
| APT-05 | Scheduled | SCHEDULED meeting is completed | State becomes HCM_MET_COMPLETED; outcome retained | API test |
| APT-06 | Scheduled | Attempt reverse or duplicate transition | Request is rejected and state is unchanged | `AppointmentLifecycleServiceTest` |
| APT-07 | Scheduled | Approver returns request for information | PENDING remains visible; reason/info/due date retained | API/UI test |
| APT-08 | Scheduled | Applicant/DEO resubmits returned request | Return fields clear; audit event is recorded | API test |
| WALK-01 | Walk-in | Register a walk-in | Initial state is PENDING and category WALK_IN | API test |
| WALK-02 | Walk-in | Complete a walk-in | State becomes COMPLETED | Unit/API test |
| WALK-03 | Walk-in | Apply scheduled-only state to walk-in | Request is rejected | `AppointmentLifecycleServiceTest` |
| PD-01 | Public Darbar | Exercise existing Public Darbar flow | Existing appointment type/workflow remains operational | Regression suite/manual E2E |
| FU-01 | Follow-up | Create direction after meeting/routing | Unique DIR reference, owner, due date and priority persist | API test |
| FU-02 | Follow-up | Progress PENDING → IN_PROGRESS → COMPLETED | Valid transitions succeed and are audited | API test |
| FU-03 | Follow-up | Due date passes while open | Derived state is OVERDUE and escalation is emitted once per interval | Scheduler/API test |
| FU-04 | Follow-up | Department admin queries follow-ups | Only own-department records are returned | Security/API test |
| FU-05 | Evidence | Upload invalid or unsafe evidence | Upload rejected by existing size/type/MIME/malware validation | API test |
| FU-06 | Evidence | Upload valid evidence | Document is securely stored and linked to direction | API test |
| HCM-01 | Dashboard | Open HCM dashboard | Separate pending, upcoming, walk-in, completed, routed, rejected and follow-up counts display | Web/mobile build + E2E |
| HCM-02 | Intelligence | Open a citizen profile | Masked profile, visits, schemes, interactions and follow-ups aggregate without N+1 loading | API test |
| HCM-03 | Intelligence | AI provider is unavailable | Deterministic stored-data summary is returned with `aiGenerated=false` | API test |
| FACE-01 | Identification | Search using face workflow | Existing identification service is reused; no duplicate biometric store exists | Regression/manual device test |
| REP-01 | Reports | Combine date/status/category/follow-up filters | Server applies all filters and returns a paged result | API test |
| REP-02 | Reports | Department admin exports | Export is restricted to department scope | Security/API test |
| REP-03 | Reports | Export PDF | Valid PDF downloads and action is audited | API/UI test |
| REP-04 | Reports | Export Excel | Valid XLSX downloads and action is audited | API/UI test |
| UI-01 | Approver web | Open PENDING item | Return, Schedule, Route and Reject actions are available | Angular build/E2E |
| UI-02 | HCM web | Open dashboard/citizen intelligence | Decision-support counts and consolidated history render | Angular build/E2E |
| MOB-01 | Approver mobile | Review PENDING item | Schedule, Route and Reject call canonical APIs | Flutter analyze/device E2E |
| MOB-02 | HCM mobile | Open dashboard | Server aggregate counts render; offline fallback remains usable | Flutter analyze/device E2E |
| MIG-01 | Flyway | Start an environment where V34 already succeeded | No applied migration is edited; only new V70+ migrations run | Flyway migrate against upgraded copy |

## Release commands

```text
cd backend  && mvn test
cd frontend && npm run build
cd mobile   && dart format --output=none --set-exit-if-changed lib test
cd mobile   && flutter analyze
cd mobile   && flutter test
```

Release sign-off additionally requires executing all rows marked API/UI/device E2E against an upgraded database copy and attaching request/response or screenshot evidence. No unexecuted manual case should be reported as passed.
