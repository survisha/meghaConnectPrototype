# Mobile Face Identification Pilot Regression Tests

These device tests require an authorized test account, camera permission, the pilot API, one enrolled citizen, and one non-enrolled test subject. Never record biometric payloads in test evidence.

| ID | Scenario | Expected result |
|---|---|---|
| FACE-REG-01 | Open Angular Walk-in Counter | Existing screen and face/manual lookup controls work unchanged. |
| FACE-REG-02 | Capture an enrolled citizen in Angular Walk-in | Existing citizen is identified. |
| FACE-REG-03 | Open Angular Public Identification | Camera control opens normally. |
| FACE-REG-04 | Present a face to Angular Public Identification | Existing automatic capture behavior runs. |
| FACE-REG-05 | Match in Angular Public Identification | Citizen history displays. |
| FACE-REG-06 | Open Mobile Walk-in Counter | Front camera opens automatically. |
| FACE-REG-07 | Present enrolled citizen in Mobile Walk-in | Face is automatically captured once. |
| FACE-REG-08 | Complete Mobile Walk-in face search | Existing registered citizen is identified. |
| FACE-REG-09 | Inspect matched Mobile Walk-in citizen | Profile and photo display. |
| FACE-REG-10 | Continue matched Mobile Walk-in citizen | Existing appointment workflow opens with the selected visitor. |
| FACE-REG-11 | Present unknown citizen in Mobile Walk-in | Existing EPIC/registration fallback is shown. |
| FACE-REG-12 | Register unknown citizen | Existing duplicate controls remain active; no existing citizen is duplicated. |
| FACE-REG-13 | Open Mobile Public Identification | Front camera opens automatically. |
| FACE-REG-14 | Present enrolled citizen | Face automatically captures without a Capture button. |
| FACE-REG-15 | Observe network calls for one face | `/face-recognition/search` is called once for the tracked face. |
| FACE-REG-16 | Complete registered-citizen match | Correct result is selected automatically. |
| FACE-REG-17 | Observe matched result | `/public-identification/citizens/{id}/full-history` loads. |
| FACE-REG-18 | Citizen has prior visits | Visit count and last visit display. |
| FACE-REG-19 | Citizen has prior appointments | Appointment history displays. |
| FACE-REG-20 | Citizen has a profile photo | Correct authenticated photo displays. |
| FACE-REG-21 | Keep the same face in frame | No continuous duplicate captures/searches occur. |
| FACE-REG-22 | Allow search to finish | Processing lock clears and UI remains responsive. |
| FACE-REG-23 | Back out and reopen Public Identification | A fresh camera session opens and auto-capture works. |
| FACE-REG-24 | Back out and reopen Walk-in | A fresh camera session opens and auto-capture works. |
| FACE-REG-25 | Alternate between both menu entries | Mode-specific result actions do not leak between screens. |
| FACE-REG-26 | Simulate face API error/timeout | Graceful unavailable/timeout result displays without a crash. |
| FACE-REG-27 | Deny camera permission | Permission guidance displays without a crash. |
| FACE-REG-28 | Leave camera frame empty | No face-search request is made. |
| FACE-REG-29 | Keep the same person across repeated capture intervals | Tracking gate prevents repeated face-search requests. |
| FACE-REG-30 | Inspect Flutter face request | JSON request contains normalized JPEG Base64 and authenticated `application/json` headers. |
| FACE-REG-31 | Run Angular checks after deployment | Existing Angular flows remain unchanged. |
| FACE-REG-32 | Verify role/login access | Existing authentication and authorization remain unchanged. |

## Mandatory device smoke sequence

Run the four sequences from the pilot brief: normal identification, matched Walk-in, unknown Walk-in, and Walk-in → back → Public Identification → back → Walk-in. Capture timestamps and non-sensitive screenshots; do not capture Base64 images, tokens, EPIC values, or mobile numbers in logs.
