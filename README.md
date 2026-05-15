# meghaConnectPrototype
I want to create the proto type for Meghalaya Entry-Exit and handling cm schemes UI and backend structure and DB schemas to show a demo for approval of the design

## QR scanner API examples

Create a security scanner user as an admin:

```bash
curl -X POST "https://meghaconnect.cloud/api/v1/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-create-security-001" \
  -d '{
    "username": "main_gate_security",
    "password": "ChangeMe123!",
    "fullName": "Main Gate Security",
    "role": "SECURITY",
    "phoneNumber": "9999999999",
    "active": true
  }'
```

Login with the existing auth API:

```bash
curl -X POST "https://meghaconnect.cloud/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"main_gate_security","password":"ChangeMe123!"}'
```

Validate a visitor QR token from the Android scanner app. The QR contains only the raw secure token; the backend stores only its SHA-256 hash.

```bash
curl -X POST "https://meghaconnect.cloud/api/v1/qr/validate" \
  -H "Authorization: Bearer $SECURITY_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: scan-validate-001" \
  -d '{
    "qrToken": "RAW_QR_TOKEN_FROM_APPROVAL",
    "deviceId": "android-scanner-001",
    "gateName": "Main Gate"
  }'
```

Check in and check out:

```bash
curl -X POST "https://meghaconnect.cloud/api/v1/qr/check-in" \
  -H "Authorization: Bearer $SECURITY_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: scan-entry-001" \
  -d '{"qrToken":"RAW_QR_TOKEN_FROM_APPROVAL","deviceId":"android-scanner-001","gateName":"Main Gate"}'

curl -X POST "https://meghaconnect.cloud/api/v1/qr/check-out" \
  -H "Authorization: Bearer $SECURITY_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: scan-exit-001" \
  -d '{"qrToken":"RAW_QR_TOKEN_FROM_APPROVAL","deviceId":"android-scanner-001","gateName":"Main Gate"}'
```

Admin reports:

```bash
curl "https://meghaconnect.cloud/api/v1/qr/audit-logs?fromDate=2026-05-15&toDate=2026-05-15&gateName=Main%20Gate" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl "https://meghaconnect.cloud/api/v1/qr/movements?date=2026-05-15&gateName=Main%20Gate&status=ENTRY" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```
