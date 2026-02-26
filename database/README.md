# Database Setup

## PostgreSQL Setup

```sql
CREATE DATABASE meghaconnect;
CREATE USER megha_user WITH PASSWORD 'megha_pass';
GRANT ALL PRIVILEGES ON DATABASE meghaconnect TO megha_user;
```

## Tables

| Table | Description |
|-------|-------------|
| `users` | System users (HCM, Admin, CMO, DEO, Public) |
| `persons` | Citizen profiles (applicants, associates) |
| `schedule_events` | Calendar events (A1-A4, B1-B2) |
| `appointments` | All appointment requests & workflow |
| `directions` | HCM colour-coded directions (Green/Yellow/Blue) |
| `associate_mappings` | People accompanying applicants |
| `scheme_applications` | CM Scheme applications (CMSDF, CMSG, etc.) |
| `scheme_application_items` | Itemwise cost breakdown |
| `audit_logs` | Complete system audit trail |
