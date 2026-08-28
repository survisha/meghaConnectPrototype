# Appointment Lifecycle Regression Cases

| Test Case ID | Module | Scenario / Test Case | Test Steps | Expected Result | Priority | Default Assigned To | Active |
|---|---|---|---|---|---|---|---|
| REG-REJ-001 | Backend/API | Reject active appointment with reason | Login as APPROVER; reject an active appointment with `rejectionReason`; fetch by ID. | `REJECTED`; `rejectedAt` non-null; `rejectedBy` is authenticated user; reason retained. | Critical | API QA | Yes |
| REG-REJ-002 | Database | Verify rejection columns | Query the rejected appointment row. | `rejected_at`, `rejected_by`, and `rejection_reason` are persisted. | Critical | DB QA | Yes |
| REG-REJ-003 | Audit | Verify rejection transition | Inspect status history after rejection. | Old status to `REJECTED`, reason, actor, role, and timestamp recorded. | Critical | Audit QA | Yes |
| REG-APPT-FILTER-001 | Appointments | Active normal filtering | Create normal PENDING, walk-in PENDING, and normal SCHEDULED; open All Appointments. | Both normal rows visible; `B2 Walk-in` excluded. | Critical | UI/API QA | Yes |
| REG-APPT-FILTER-002 | Appointments | Rejected normal excluded | Create a normal REJECTED record; open All Appointments. | Rejected record is absent. | High | UI/API QA | Yes |
| REG-WALK-FILTER-001 | Walk-in | Pending included | Create `B2 Walk-in` + PENDING; open Walk-in list. | Record visible. | Critical | UI/API QA | Yes |
| REG-WALK-FILTER-002 | Walk-in | Pending request included | Create `B2 Walk-in` + PENDING_REQUEST; open Walk-in list. | Record visible. | Critical | UI/API QA | Yes |
| REG-WALK-FILTER-003 | Walk-in | Completed excluded | Create `B2 Walk-in` + COMPLETED; open Walk-in list. | Record absent. | High | UI/API QA | Yes |
| REG-WALK-FILTER-004 | Walk-in | Rejected excluded | Create `B2 Walk-in` + REJECTED; open Walk-in list. | Record absent. | High | UI/API QA | Yes |
| REG-WALK-FILTER-005 | Walk-in | Normal excluded | Create normal PENDING; open Walk-in list. | Record absent. | High | UI/API QA | Yes |
| REG-MISSING-001 | Workflow | Walk-in missing information | Login as APPROVER; open PENDING walk-in. | Request Missing Information enabled. | High | UI QA | Yes |
| REG-MISSING-002 | Workflow | Walk-in transition | Submit missing-information request. | Status becomes PENDING_REQUEST; audit updated. | Critical | API QA | Yes |
| REG-MISSING-003 | Workflow | Scheduled normal eligibility | Open normal SCHEDULED appointment as HCM. | Request Missing Information enabled. | High | UI QA | Yes |
| REG-MISSING-004 | Workflow | Scheduled normal transition | Submit missing-information request. | SCHEDULED becomes PENDING_REQUEST. | Critical | API QA | Yes |
| REG-MISSING-005 | Workflow | Terminal eligibility | Open COMPLETED, CLOSED, and REJECTED records. | Missing-information action unavailable and backend rejects transition. | Critical | Security QA | Yes |
| REG-COMPLETE-001 | Workflow | Complete walk-in | Open PENDING walk-in after saving remarks. | Complete enabled. | Critical | UI QA | Yes |
| REG-COMPLETE-002 | Workflow | Block requested walk-in | Open PENDING_REQUEST walk-in. | Complete disabled; backend rejects completion. | Critical | API QA | Yes |
| REG-COMPLETE-003 | Workflow | Block pending normal | Open normal PENDING appointment. | Complete disabled; backend rejects completion. | Critical | API QA | Yes |
| REG-COMPLETE-004 | Workflow | Complete scheduled normal | Open normal SCHEDULED appointment after saving remarks. | Complete enabled. | Critical | UI QA | Yes |
| REG-COMPLETE-005 | Workflow | Complete rescheduled normal | Open normal RESCHEDULED appointment. | Complete enabled. | Critical | UI QA | Yes |
| REG-COMPLETE-006 | Backend/API | Completion metadata | Click Complete; fetch by ID and inspect audit. | Status COMPLETED; completion actor/time and transition audit persisted. | Critical | API QA | Yes |
| REG-FOLLOWUP-001 | Completed | Inline follow-up panel | Open Completed detail; click Add Follow-up. | Inline side-panel form opens; no prompt/modal. | High | UI QA | Yes |
| REG-FOLLOWUP-002 | Completed | Save follow-up | Enter remarks and save. | History updated; appointment remains COMPLETED. | Critical | UI/API QA | Yes |
| REG-CLOSE-001 | Completed | Inline close panel | Click Close in Completed detail. | Inline close form opens. | High | UI QA | Yes |
| REG-CLOSE-002 | Completed | Close transition | Enter final remarks and confirm. | Status CLOSED; close metadata persisted; row leaves Completed list. | Critical | UI/API QA | Yes |
| REG-CLOSED-001 | Closed | Closed-only list | Open Closed Appointments. | Only CLOSED records appear. | Critical | UI/API QA | Yes |
| REG-CLOSED-002 | Closed | Completed excluded | Ensure a COMPLETED record exists; open Closed list. | COMPLETED record absent. | High | UI/API QA | Yes |
| REG-CLOSED-003 | Backend/API | Direct closed filter | GET appointments with `status=CLOSED`. | Every returned row has status CLOSED. | Critical | API QA | Yes |
| API-REG-001 | Backend/API | Walk-in predicates | GET with statuses PENDING/PENDING_REQUEST and `appointmentType=B2 Walk-in`. | Only matching walk-ins returned; filter precedes pagination. | Critical | API QA | Yes |
| API-REG-002 | Backend/API | Normal active predicates | GET active statuses with `appointmentType=NORMAL`. | `B2 Walk-in` excluded; only requested statuses returned. | Critical | API QA | Yes |
| API-REG-003 | Backend/API | Single closed status | GET with `status=CLOSED`. | Only CLOSED rows returned. | Critical | API QA | Yes |
| API-REG-004 | Backend/API | Rejection contract aliases | Reject once with `rejectionReason`, then exercise supported legacy reason field. | Reason is never silently discarded; authenticated principal supplies actor. | Critical | API QA | Yes |
