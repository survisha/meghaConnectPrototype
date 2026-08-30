# MeghaConnect Pilot Regression Tests

Environment prerequisite: use authorized test accounts for Approver/HCM, DEO, Citizen, and Security roles; seed appointments must belong to the test users and department tenancy under test.

| ID | Test | Expected result |
|---|---|---|
| REG-01 | Open All Appointments at common laptop/desktop widths | Every permitted action icon is visible without clipping. |
| REG-02 | Inspect Actions column | View History is visible with tooltip. |
| REG-03 | Click View History | Correct visitor history opens. |
| REG-04 | Inspect appointment actions | CMO Modify is absent. |
| REG-05 | Inspect appointment actions | Forward to Approver is absent. |
| REG-06 | Save appointment remarks | Remarks save successfully. |
| REG-07 | Save remarks on a non-completed appointment | Status and completion metadata remain unchanged. |
| REG-08 | Refresh after saving remarks | Remarks persist and status remains unchanged. |
| REG-09 | Click Complete and pass existing validation | Status becomes COMPLETED. |
| REG-10 | Reschedule a SCHEDULED appointment to a valid slot | Existing appointment is updated successfully. |
| REG-11 | Refresh after rescheduling | New date/time persists. |
| REG-12 | Reschedule to past/conflicting slot | Existing validation rejects the request. |
| REG-13 | View appointment photo in Angular | Photo continues to display. |
| REG-14 | View raw Base64 photo in Flutter | Photo displays from memory. |
| REG-15 | View JPEG data URI in Flutter | JPEG displays from memory. |
| REG-16 | View PNG data URI in Flutter | PNG displays from memory. |
| REG-17 | View relative upload URL in Flutter | URL resolves against configured server origin and displays with auth headers. |
| REG-18 | View null/malformed photo in Flutter | Placeholder displays without a crash. |
| REG-19 | Approver requests additional information with remarks | Remarks persist and status becomes PENDING_REQUEST. |
| REG-20 | Authorized DEO opens PENDING_REQUEST | Request for Additional Information is clearly visible. |
| REG-21 | Citizen opens own PENDING_REQUEST | Request for Additional Information is clearly visible. |
| REG-22 | DEO/Citizen requests an unauthorized appointment | Existing authorization denies access; internal remarks are not exposed. |
| REG-23 | Tap Rejected Appointments menu | Rejected report screen opens. |
| REG-24 | Load rejected report | Correct rejected records load from existing report API. |
| REG-25 | View a rejected record | Correct read-only details and photo display. |
| REG-26 | Tap Completed Appointments menu | Completed report screen opens. |
| REG-27 | Load completed report | Correct completed records load from existing report API. |
| REG-28 | View a completed record | Correct read-only details and photo display. |
| REG-29 | Tap QR Scanner menu as DEO | Existing scanner opens. |
| REG-30 | Scan a valid visitor-pass QR | Existing validation/processing flow runs. |
| REG-31 | Deny camera permission | Scanner shows graceful guidance/error and does not crash. |
| REG-32 | Inspect menus for each role | Existing role restrictions remain intact. |
| REG-33 | Login and logout after changes | Existing authentication flow succeeds. |
| REG-34 | Refresh appointment list and navigate repeatedly | No duplicate calls, records, or navigation loops occur. |

## Mandatory smoke sequence

Run the three role-based sequences from the pilot brief against the deployed pilot API: appointment photo/history/remarks/completion; SCHEDULED reschedule; and Approver additional-information followed by DEO/Citizen visibility. On mobile also open Appointments, Rejected, Completed, and QR Scanner. Record appointment IDs, roles, timestamps, and screenshots for release evidence.
