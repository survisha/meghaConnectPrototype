# meghaConnectPrototype
I want to create the proto type for Meghalaya Entry-Exit and handling cm schemes UI and backend structure and DB schemas to show a demo for approval of the design

## Pilot/Production Deployment - MeghaConnect

This section documents the separate Pilot/Production deployment for `https://www.meghaconnect.com`. It does not replace the existing UAT deployment. UAT continues to use `deploy.sh` and `https://meghaconnect.cloud/api/v1`.

### UAT vs Production

| Item | UAT | Pilot/Production |
| --- | --- | --- |
| Deploy script | `deploy.sh` | `deploy-prod.sh` |
| Frontend URL | `https://meghaconnect.cloud` | `https://www.meghaconnect.com` |
| API URL | `https://meghaconnect.cloud/api/v1` | `https://www.meghaconnect.com/api/v1` |
| Angular build config | `uat` | `production` |
| Spring profile | `dev` in current UAT script | `prod` |
| Backend service | `meghaconnect-api` | `meghaconnect-api-prod` |
| Backend local port | `8080` | `8082` |
| Database | `meghaconnect_db` | `meghaconnect_prod` |
| Backend JAR destination | `/opt/meghaconnect/backend/app.jar` | `/opt/meghaconnect/prod/backend/app.jar` |
| Frontend destination | `/var/www/meghaconnect` | `/var/www/meghaconnect-prod` |
| Logs | `/opt/meghaconnect/logs` | `/var/log/meghaconnect/prod` |
| Env file | `/etc/meghaconnect/meghaconnect-api.env` | `/etc/meghaconnect/meghaconnect-api-prod.env` |

### Files Added For Production

- `deploy-prod.sh`
- `frontend/src/environments/environment.prod.ts`
- `backend/src/main/resources/application-prod.yml`
- `deployment/nginx/meghaconnect-prod.conf`

The existing UAT `deploy.sh` should remain untouched for production rollout.

### Production Frontend Environment

Production Angular builds use:

```ts
apiUrl: '/api/v1'
```

The production UI uses a same-origin API path so both
`https://meghaconnect.com` and `https://www.meghaconnect.com` call their own
host without a browser CORS hop.

The replacement is configured in `frontend/angular.json` so this command uses `environment.prod.ts`:

```bash
cd frontend
npm ci
npx ng build --configuration production
```

Build output source:

```text
frontend/dist/frontend/
frontend/dist/frontend/browser/   # if Angular emits browser/ output
```

Deployment destination:

```text
/var/www/meghaconnect-prod/
```

### Production Backend Configuration

Production backend profile file:

```text
backend/src/main/resources/application-prod.yml
```

Important defaults:

```text
server.port=8082
database=meghaconnect_prod
upload path=/opt/meghaconnect/prod/uploads
crypto key path=/opt/meghaconnect/prod/secure/crypto.key
logs=/var/log/meghaconnect/prod
CORS=https://www.meghaconnect.com,https://meghaconnect.com
```

Secrets must be supplied through environment variables or `/etc/meghaconnect/meghaconnect-api-prod.env`:

```text
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
SMS/WhatsApp credentials, if enabled
third-party credentials, if enabled
```

Do not hardcode production secrets in source files.

Backend JAR source:

```text
backend/target/<generated-app>.jar
```

Backend JAR destination:

```text
/opt/meghaconnect/prod/backend/app.jar
```

### Production Database

Create a separate production database. Do not point production to the UAT database.

```sql
CREATE DATABASE meghaconnect_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'meghaconnect_prod_user'@'localhost' IDENTIFIED BY 'CHANGE_THIS_PASSWORD';
GRANT ALL PRIVILEGES ON meghaconnect_prod.* TO 'meghaconnect_prod_user'@'localhost';
FLUSH PRIVILEGES;
```

Before deployment, verify `/etc/meghaconnect/meghaconnect-api-prod.env` points to a DB name ending with `_prod`. Flyway migrations are enabled by the prod profile and should run against `meghaconnect_prod`.

### Run Production Deployment

From the repository root on the server:

```bash
chmod +x deploy-prod.sh
./deploy-prod.sh
```

The script will:

1. Validate that production API URL is `https://www.meghaconnect.com/api/v1`.
2. Validate that the DB name ends with `_prod`.
3. Validate that production port is not the UAT port.
4. Build Angular with `ng build --configuration production`.
5. Build backend with `mvn clean package -DskipTests`.
6. Backup the current production frontend and backend JAR.
7. Copy frontend files to `/var/www/meghaconnect-prod/`.
8. Copy backend JAR to `/opt/meghaconnect/prod/backend/app.jar`.
9. Restart only `meghaconnect-api-prod`.
10. Install/reload the production Nginx site.
11. Run a local health check.

For prebuilt artifacts:

```bash
DEPLOY_MODE=prebuilt \
PREBUILT_FRONTEND_DIR=/path/to/release-prod/frontend \
PREBUILT_BACKEND_JAR=/path/to/release-prod/backend/app.jar \
./deploy-prod.sh
```

If the deploy script itself is already placed inside the release folder, this
layout is also accepted, even if the script is inside one nested `release-prod`
folder:

```text
/opt/release-prod/frontend/index.html
/opt/release-prod/backend/app.jar
```

In that case run:

```bash
cd /opt/release-prod
unset PREBUILT_FRONTEND_DIR PREBUILT_BACKEND_JAR PROJECT_ROOT
DEPLOY_MODE=prebuilt ./deploy-prod.sh
```

You can also point the script at a release folder explicitly:

```bash
ARTIFACT_ROOT=/opt/release-prod DEPLOY_MODE=prebuilt ./deploy-prod.sh
```

### Production Nginx

Reference config:

```text
deployment/nginx/meghaconnect-prod.conf
```

Installed server config:

```text
/etc/nginx/sites-available/meghaconnect-prod
/etc/nginx/sites-enabled/meghaconnect-prod
```

Production frontend root:

```text
/var/www/meghaconnect-prod
```

Production API proxy:

```text
https://www.meghaconnect.com/api/v1 -> http://127.0.0.1:8082/api/v1
```

Minimal Nginx block:

```nginx
server {
    listen 80;
    server_name meghaconnect.com www.meghaconnect.com;

    root /var/www/meghaconnect-prod;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/v1/ {
        proxy_pass http://127.0.0.1:8082/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

To enable SSL with Certbot after DNS points to the server:

```bash
sudo certbot --nginx -d meghaconnect.com -d www.meghaconnect.com
```

Or let the script request it:

```bash
ENABLE_CERTBOT=true CERTBOT_EMAIL=admin@meghaconnect.com ./deploy-prod.sh
```

### Verify Production

```bash
curl http://127.0.0.1:8082/api/actuator/health
curl https://www.meghaconnect.com/api/actuator/health
sudo systemctl status meghaconnect-api-prod
sudo journalctl -u meghaconnect-api-prod -f
sudo tail -f /var/log/meghaconnect/prod/meghaconnect-prod.log
```

Open:

```text
https://www.meghaconnect.com
```

Confirm browser API calls go to:

```text
https://www.meghaconnect.com/api/v1
```

### Restart Production

```bash
sudo systemctl restart meghaconnect-api-prod
sudo systemctl status meghaconnect-api-prod
```

### Rollback

Backend JAR backups are stored in:

```text
/opt/meghaconnect/prod/backups/backend/
```

Frontend backups are stored in:

```text
/var/www/meghaconnect-prod-backups/
```

Rollback backend:

```bash
sudo systemctl stop meghaconnect-api-prod
sudo cp /opt/meghaconnect/prod/backups/backend/app-YYYYMMDDHHMMSS.jar /opt/meghaconnect/prod/backend/app.jar
sudo chown meghaconnect:meghaconnect /opt/meghaconnect/prod/backend/app.jar
sudo systemctl start meghaconnect-api-prod
```

Rollback frontend:

```bash
sudo find /var/www/meghaconnect-prod -mindepth 1 -maxdepth 1 -exec rm -rf {} +
sudo tar -xzf /var/www/meghaconnect-prod-backups/frontend-prod-YYYYMMDDHHMMSS.tar.gz -C /var/www/meghaconnect-prod
sudo chown -R www-data:www-data /var/www/meghaconnect-prod
sudo systemctl reload nginx
```

Safety note: production rollback commands affect only production paths and `meghaconnect-api-prod`. UAT `deploy.sh`, `/var/www/meghaconnect`, `/opt/meghaconnect/backend`, and `meghaconnect-api` are not touched.

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

## AI provider and model configuration

The backend AI calls are routed through configuration. To change the AI model,
do not edit Java code. Change environment variables or the matching
`application*.yml` keys, then restart the backend service.

Common keys in all backend profile files:

```yaml
ai:
  provider: ${AI_PROVIDER:ollama}
  timeout-seconds: ${AI_TIMEOUT_SECONDS:60}
  ollama:
    enabled: ${OLLAMA_ENABLED:true}
    base-url: ${OLLAMA_BASE_URL:http://127.0.0.1:11434}
    model: ${OLLAMA_MODEL:llama3.2}
    generate-endpoint: ${OLLAMA_GENERATE_ENDPOINT:/api/generate}
    timeout-seconds: ${AI_TIMEOUT_SECONDS:60}
    max-input-chars: ${AI_MAX_INPUT_CHARS:12000}
  openai:
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    api-key: ${OPENAI_API_KEY:}
    model: ${OPENAI_MODEL:gpt-4o-mini}
  azure-openai:
    endpoint: ${AZURE_OPENAI_ENDPOINT:}
    api-key: ${AZURE_OPENAI_API_KEY:}
    deployment: ${AZURE_OPENAI_DEPLOYMENT:}
```

For the current Ollama setup, change only the model env var:

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://127.0.0.1:11434
export OLLAMA_MODEL=llama3.2
```

Example model switch:

```bash
ollama pull mistral
export OLLAMA_MODEL=mistral
sudo systemctl restart meghaconnect-api
```

For UAT/production systemd deployments, put the values in the service env file:

```text
/etc/meghaconnect/meghaconnect-api.env
/etc/meghaconnect/meghaconnect-api-prod.env
```

Then restart the relevant service:

```bash
sudo systemctl restart meghaconnect-api
sudo systemctl restart meghaconnect-api-prod
```

Verify the selected provider and model:

```bash
curl "https://meghaconnect.cloud/api/ai/health" -H "Authorization: Bearer $OFFICER_TOKEN"
curl "https://www.meghaconnect.com/api/ai/health" -H "Authorization: Bearer $OFFICER_TOKEN"
```

`AI_PROVIDER=openai` and `AI_PROVIDER=azure-openai` are configuration placeholders
for future provider implementations. The current working provider is `ollama`.

## Ollama AI document notes verification

Configure Ollama for backend-only AI notes generation:

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama3.2
```

Verification checklist:

1. Start Ollama on the backend server and make sure the configured model is pulled.
2. Upload a PDF/DOCX/TXT document through the existing appointment document upload flow.
3. Confirm the upload API still returns immediately and the document can still be viewed/downloaded.
4. Confirm `appointment_document_ai_notes` receives a `PENDING` row for the uploaded document.
5. Confirm the row changes to `PROCESSING` and then `COMPLETED`.
6. Open the Angular Appointments list and verify the AI Notes column shows status, summary preview, full notes modal, and regenerate action.
7. Stop Ollama, upload another document, and verify upload still succeeds while AI notes move to `FAILED`.
8. Regenerate notes for a document:

```bash
curl -X POST "https://meghaconnect.cloud/api/v1/appointments/documents/$DOCUMENT_ID/ai-notes/regenerate" \
  -H "Authorization: Bearer $OFFICER_TOKEN" \
  -H "X-Request-Id: ai-notes-regenerate-001"
```

Fetch notes for an appointment:

```bash
curl "https://meghaconnect.cloud/api/v1/appointments/$APPOINTMENT_ID/ai-notes" \
  -H "Authorization: Bearer $OFFICER_TOKEN" \
  -H "X-Request-Id: ai-notes-list-001"
```
