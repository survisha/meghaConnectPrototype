# Appointment and Walk-in Lifecycle QA

| Test Case ID | Module | Scenario | Preconditions | Role | Steps | Input | Expected Result | Actual Result | Pass/Fail | Severity | Remarks |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-APT-STATE-001 | Creation | Citizen creates scheduled appointment | Verified citizen | CITIZEN | Submit appointment | Valid request | Category SCHEDULED, status PENDING; creation history stored | Pending execution | — | Critical | |
| TC-APT-STATE-002 | Creation | DEO creates scheduled appointment | Visitor exists | DEO | Create non-walk-in | isWalkIn=false | Category SCHEDULED, status PENDING | Pending execution | — | Critical | |
| TC-APT-STATE-003 | Creation | DEO creates Walk-in | Visitor exists | DEO | Create Walk-in | isWalkIn=true | Appointment and Walk-in rows PENDING | Pending execution | — | Critical | |
| TC-APT-STATE-004 | Approver | Pending scheduled visibility | Scheduled PENDING exists | APPROVER | Open inbox | — | Appears in pending scheduled queue | Pending execution | — | High | |
| TC-APT-STATE-005 | Scheduling | Schedule pending appointment | Scheduled PENDING exists | APPROVER | Select Schedule and save | Future slot | Status SCHEDULED; history recorded | Pending execution | — | Critical | |
| TC-APT-STATE-006 | Rejection | Reject pending appointment | Scheduled PENDING exists | APPROVER | Reject with reason | Valid reason | Status REJECTED; rejectedAt/by/history populated | Pending execution | — | Critical | |
| TC-APT-STATE-007 | Return | Return for information | Scheduled PENDING exists | APPROVER | Return and resubmit | Reason/details | Remains canonical PENDING with return/resubmit audit metadata | Pending execution | — | High | Same-status implementation retained |
| TC-APT-STATE-008 | Route | Route and close | Scheduled PENDING exists | APPROVER | Select department and route | Department/direction | ROUTED_TO_OFFICIAL; history stored | Pending execution | — | High | |
| TC-APT-STATE-009 | HCM | Scheduled appointment visibility | Scheduled SCHEDULED exists | HCM | Open dashboard | Date | Appears under scheduled appointments | Pending execution | — | High | |
| TC-APT-STATE-010 | Completion | Complete scheduled meeting | Scheduled SCHEDULED with outcome | HCM | Complete meeting | Outcome/remarks | HCM_MET_COMPLETED; history stored | Pending execution | — | Critical | |
| TC-APT-STATE-011 | Validation | Direct pending completion | Scheduled PENDING exists | HCM | Call completion API | Outcome | Controlled invalid-transition response | Automated policy test | Pass | Critical | |
| TC-WALK-STATE-001 | Walk-in | Initial state | Valid Walk-in form | DEO | Submit | isWalkIn=true | PENDING | Automated policy/creation coverage | Pass | Critical | |
| TC-WALK-STATE-002 | Approver | Walk-in dashboard separation | Walk-in PENDING exists | APPROVER | Open dashboard | — | Appears as Live Walk-in, not Pending Scheduled | Pending execution | — | High | |
| TC-WALK-STATE-003 | HCM | HCM Walk-in visibility | Walk-in PENDING exists | HCM | Open dashboard | — | Appears under Live Walk-ins | Pending execution | — | High | |
| TC-WALK-STATE-004 | Angular/Mobile | No Schedule action | Walk-in PENDING exists | APPROVER | View actions | — | Schedule hidden | Automated Angular policy | Pass | Critical | |
| TC-WALK-STATE-005 | API | Cannot schedule Walk-in | Walk-in PENDING exists | APPROVER | Call schedule API | Future slot | Controlled invalid-transition response | Automated policy test | Pass | Critical | |
| TC-WALK-STATE-006 | Completion | Complete Walk-in | Walk-in PENDING with outcome | HCM | Complete | Outcome | Appointment and Walk-in status COMPLETED | Pending execution | — | Critical | |
| TC-WALK-STATE-007 | Validation | Complete twice | Walk-in COMPLETED exists | HCM | Call completion again | Outcome | Controlled invalid-transition response | Automated policy test | Pass | Critical | |
| TC-WALK-STATE-008 | Reporting | Completed Walk-in report | Walk-in COMPLETED exists | HCM | Open Completed Appointments | — | Included as WALK_IN/COMPLETED | Pending execution | — | High | |
| TC-WALK-STATE-009 | Reporting | Walk-in excluded from rejected | Walk-in records exist | HCM | Open Rejected Appointments | — | No Walk-in rows | Pending execution | — | High | |
| TC-STATE-SEC-001 | Security | DEO schedules | Scheduled PENDING exists | DEO | Call schedule endpoint | Valid slot | HTTP 403 | Pending execution | — | Critical | |
| TC-STATE-SEC-002 | Security | DEO rejects | Scheduled PENDING exists | DEO | Call reject endpoint | Reason | HTTP 403 | Pending execution | — | Critical | |
| TC-STATE-SEC-003 | Security | Approver schedules Walk-in | Walk-in PENDING exists | APPROVER | Call schedule endpoint | Valid slot | Invalid-transition response | Automated policy test | Pass | Critical | |
| TC-STATE-SEC-004 | Creation security | Manipulated initial status | Valid create request | DEO | Include status COMPLETED | status=COMPLETED | Server persists PENDING | Pending execution | — | Critical | |
| TC-STATE-SEC-005 | State machine | Generic API bypass attempt | Canonical record exists | APPROVER | PATCH invalid status | Invalid transition | Request rejected by lifecycle service | Automated policy test | Pass | Critical | |
| TC-STATE-DASH-001 | Dashboard | Pending scheduled count | Mixed PENDING data | HCM | Load summary | — | Counts only SCHEDULED/PENDING | Pending execution | — | High | |
| TC-STATE-DASH-002 | Dashboard | Live Walk-in count | Mixed PENDING data | HCM | Load summary | — | Counts only live Walk-ins | Pending execution | — | High | |
| TC-STATE-DASH-003 | Dashboard | Completed count | Both completed categories exist | HCM | Load summary | — | Includes valid completed categories only | Pending execution | — | High | |
| TC-STATE-HIST-001 | History | Creation audit | New appointment created | Authorized creator | View history | — | NONE to PENDING entry | Pending execution | — | High | |
| TC-STATE-HIST-002 | History | Schedule audit | Scheduled transition completed | APPROVER | View history | — | PENDING to SCHEDULED entry | Pending execution | — | High | |
| TC-STATE-HIST-003 | History | Walk-in completion audit | Walk-in completed | HCM | View history | — | PENDING to COMPLETED entry | Pending execution | — | High | |
| TC-STATE-HIST-004 | History | Rejection audit | Appointment rejected | APPROVER | View history | — | PENDING to REJECTED entry | Pending execution | — | High | |
