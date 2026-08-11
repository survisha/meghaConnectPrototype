# EPIC Face Integration QA Cases

| Test Case ID | Module | Scenario | Preconditions | Role | Steps | Input | Expected Result | Actual Result | Pass/Fail | Severity | Remarks |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-EPIC-FACE-001 | Backend 1:N | Valid face match | Provider mock available | DEO | POST face search | Valid JPEG Base64 | 200, matched EPIC details | Automated service/DTO coverage | Pass | Critical | Provider UAT pending |
| TC-EPIC-FACE-002 | Backend 1:N | Valid face no-match | Provider mock returns matched=false | DEO | POST face search | Valid JPEG Base64 | 200, matched=false, NOT_FOUND | Automated | Pass | High | |
| TC-EPIC-FACE-003 | Backend 1:N | Provider error flag | Provider mock error=true | DEO | POST face search | Valid image | Safe provider error, no raw detail | Automated | Pass | High | |
| TC-EPIC-FACE-004 | Backend client | Malformed JSON | Mock server | DEO | Invoke search | Invalid JSON | Controlled INVALID_RESPONSE | Not executed | Pending | High | MockWebServer follow-up |
| TC-EPIC-FACE-005 | Backend client | HTML response | Mock server | DEO | Invoke search | HTML | Controlled INVALID_RESPONSE | Not executed | Pending | High | |
| TC-EPIC-FACE-006 | Backend client | Provider timeout | Mock server delays | DEO | Invoke search | Valid image | HTTP 504 mapping | Not executed | Pending | High | |
| TC-EPIC-FACE-007 | Validation | Blank image | Authenticated | DEO | POST search | Blank photo | HTTP 400 | Validator reused | Pass | High | Existing validator coverage |
| TC-EPIC-FACE-008 | Validation | Invalid Base64 | Authenticated | DEO | POST search | Invalid Base64 | HTTP 400 | Validator reused | Pass | High | |
| TC-EPIC-FACE-009 | Validation | Oversized image | Authenticated | DEO | POST search | Oversized Base64 | HTTP 400 | Validator reused | Pass | High | |
| TC-EPIC-FACE-010 | Configuration | API key isolation | Application configured | Admin | Inspect request/public response | Configured secret | Key injected server-side only | Code review | Pass | Critical | Test-class keys not copied |
| TC-EPIC-VERIFY-001 | Backend 1:1 | Matching EPIC and face | Provider mock | DEO | POST verify | EPIC + face | matched=true | DTO/service coverage | Pass | Critical | |
| TC-EPIC-VERIFY-002 | Backend 1:1 | Different face | Provider mock | DEO | POST verify | EPIC + face | 200, NOT_MATCHED | Automated | Pass | High | |
| TC-EPIC-VERIFY-003 | Validation | Invalid EPIC | Authenticated | DEO | POST verify | Invalid EPIC | HTTP 400 | Bean validation | Pass | Medium | |
| TC-EPIC-VERIFY-004 | Backend 1:1 | Provider failure | Provider unavailable | DEO | POST verify | Valid request | Controlled 503/504 | Not executed | Pending | High | UAT/mock-server evidence required |
| TC-EPIC-SEC-001 | Security | Unauthenticated search | None | Anonymous | POST search | Valid body | HTTP 401 | Security configuration | Pass | Critical | Endpoint not public |
| TC-EPIC-SEC-002 | Security | Unauthorized role | Auth token | PUBLIC | POST search | Valid body | HTTP 403 | Method authorization | Pass | Critical | |
| TC-EPIC-SEC-003 | Security | Secret/photo exposure | Logging enabled | DEO | Execute search | Valid body | No API key/Base64/address in logs | Code review | Pass | Critical | |
| TC-WALK-FACE-001 | Angular/Mobile | Existing visitor match | Enrolled visitor | DEO | Capture face | Known visitor | Visitor loaded; EPIC API not called | Build verified | Pending UAT | Critical | |
| TC-WALK-FACE-002 | Angular/Mobile | Local no-match, EPIC match | Provider match | DEO | Capture face | New citizen | EPIC card and prefill; live image retained | Build verified | Pending UAT | Critical | |
| TC-WALK-FACE-003 | Angular/Mobile | Both searches no-match | Providers available | DEO | Capture face | Unknown face | Visitor not found; registration offered | Build verified | Pending UAT | High | |
| TC-WALK-FACE-004 | Cascade | Local service unavailable | DeepFace unavailable | DEO | Capture face | Valid image | Controlled error/manual lookup; no automatic EPIC call | Code review | Pass | High | |
| TC-WALK-FACE-005 | Cascade | EPIC provider unavailable | Local definitive no-match | DEO | Capture face | Valid image | Existing EPIC+Name fallback offered | Build verified | Pending UAT | High | |
| TC-WALK-FACE-006 | Registration | Register EPIC face result | EPIC match | DEO | Confirm and register | Prefilled citizen | Visitor created with live image | Existing registration path | Pending UAT | Critical | |
| TC-WALK-FACE-007 | Registration | Provider image differs | EPIC match | DEO | Register | Two images | Primary visitor photo is live capture | Code review | Pass | Critical | |
| TC-WALK-FACE-008 | Registration | Existing EPIC duplicate | Visitor already exists | DEO | Register match | Existing EPIC | Duplicate rejected/visitor loaded | Existing duplicate policy | Pass | Critical | |
| TC-WALK-FACE-009 | Fallback | Manual EPIC+Name | Face no-match/failure | DEO | Select fallback | EPIC + name | Existing KYC API used unchanged | Code review | Pass | High | |
| TC-WALK-FACE-010 | Enrollment | Registration succeeds | Live image present | DEO | Register | Live capture | AFTER_COMMIT enrollment uses live image | Existing automated tests/code | Pass | Critical | |
| TC-ANG-001 | Angular | Loader finalizes for all outcomes | App running | DEO | Exercise cascade | Match/no-match/error | One terminal UI state | Build only | Pending UAT | Medium | Add component specs later |
| TC-MOB-001 | Mobile | JSON face request | App authenticated | DEO | Capture face | JPEG | application/json raw Base64 | API implementation/test baseline | Pass | High | |
| TC-MOB-002 | Mobile | Cascade cancellation | Camera session changes | DEO | Close/retake | Pending calls | Stale results ignored by session ID | Existing session guard | Pass | High | |

Rows marked Pending UAT require the configured third-party test environment and must not be represented as executed production validation.
