# Task 1 — Department administration data dictionary

## Dependency map

`users.department_id` identifies the tenant for every non-Super-Admin account. `departments` is the tenant root. `department_access_requests` records onboarding approval and links to the activated department. JWTs carry user, role, department, password-change, and credential-version context; backend services still resolve the current database user before making authorization decisions.

```text
departments 1 ─── * users
      │
      └── 0..1 department_access_requests

users ── credentials/lock state ── authentication + authorization filters
users ── performed_by ──────────── audit_logs
```

## V62 changes

### `users` columns added

| Column | Type | Purpose |
|---|---|---|
| `failed_login_attempts` | INT NOT NULL | Persistent consecutive invalid-password count |
| `last_failed_login_at` | DATETIME | Most recent invalid-password time |
| `locked_at` | DATETIME | Account lock time |
| `lock_reason` | VARCHAR(200) | Safe administrative lock reason |
| `password_changed_at` | DATETIME | Successful password replacement time |
| `temporary_password_created_at` | DATETIME | Temporary credential issue time |
| `credentials_version` | BIGINT NOT NULL | Invalidates JWTs after credential changes |
| `unlocked_by` | VARCHAR(100) | Administrative actor that unlocked the account |
| `unlocked_at` | DATETIME | Unlock time |

Existing columns reused: `department_id`, `locked`, `password_change_required`, `active`, `password_hash`, `role`, `email`, and `phone_number`.

### `department_access_requests`

Stores public onboarding requests, review status, department link, nodal contact, purpose, expected user count, review metadata, and rejection reason. The supporting-document column stores only a managed storage path; this task does not introduce file upload handling.

Indexes added:

- `users(department_id, role)`
- `users(locked, failed_login_attempts)`
- `department_access_requests(request_status, submitted_at)`
- `department_access_requests(department_code)`

Constraint added: `department_access_requests.department_id → departments.id` and positive expected user count.

## Rollback guidance

Before rollback, export `department_access_requests` and the new account-security fields for audit retention. Roll back application binaries before removing schema objects. Dropping security-state columns loses lock and credential-invalidation history and must be explicitly approved; no automated destructive down migration is supplied.

## Known legacy structures

Legacy roles such as `DATA_ENTRY_OPERATOR`, `ADMIN`, and CMO workflow roles remain for backward compatibility. New department administration uses `SUPER_ADMIN`, `DEPARTMENT_ADMIN`, `DEO`, `DEPARTMENT_PA`, and `HEAD_DEPARTMENT`; a later controlled migration may map legacy users after business validation.
