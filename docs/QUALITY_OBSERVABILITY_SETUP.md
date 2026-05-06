# MeghaConnect Quality, Security, And Observability Setup

This document covers the local/UAT setup added for:

- SonarQube
- Semgrep
- PMD and SpotBugs
- Angular ESLint
- Flutter analyzer
- Prometheus, Grafana, Loki, and Promtail
- Spring Boot JSON logs with `requestId` MDC
- Nginx routes for `/grafana`, `/logs`, and `/sonar`

## What Was Added

Quality and security:

- `sonar-project.properties`
- `.semgrepignore`
- `quality/semgrep.yml`
- Backend Maven `quality` profile with PMD and SpotBugs
- `backend/src/quality/spotbugs-exclude.xml`
- Angular ESLint config: `frontend/eslint.config.js`
- Angular lint script: `npm run lint`

Observability:

- `monitoring/docker-compose.yml`
- `monitoring/prometheus.yml`
- `monitoring/loki-config.yml`
- `monitoring/promtail-config.yml`
- Grafana datasources for Prometheus and Loki
- `backend/src/main/resources/logback-spring.xml`
- UAT log file config in `backend/src/main/resources/application-uat.yml`
- Nginx routes in `deployment/nginx/meghaconnect.conf`

## Docker Services

Start the local monitoring and quality services:

```bash
cd monitoring
docker compose up -d
```

Check services:

```bash
docker ps
docker compose logs -f grafana
docker compose logs -f loki
docker compose logs -f sonarqube
```

Default service URLs:

- Prometheus: `http://187.127.162.84:9090`
- Grafana: `http://187.127.162.84:3000`
- Loki API: `http://187.127.162.84:3100`
- SonarQube: `http://187.127.162.84:9000/sonar`

For domain/Nginx:

- Grafana: `https://meghaconnect.cloud/grafana/`
- Loki API: `https://meghaconnect.cloud/logs/`
- SonarQube: `https://meghaconnect.cloud/sonar/`

## Linux Server Notes For SonarQube

SonarQube usually needs these host settings on Linux:

```bash
sudo sysctl -w vm.max_map_count=524288
sudo sysctl -w fs.file-max=131072
ulimit -n 131072
ulimit -u 8192
```

Persist `vm.max_map_count` in `/etc/sysctl.conf` before production use.

## Grafana And Loki

Grafana is provisioned with:

- Prometheus datasource: `Prometheus`
- Loki datasource: `Loki`

Promtail tails:

- Spring Boot logs: `/opt/meghaconnect/logs/*.log`
- Nginx logs: `/var/log/nginx/*.log`

To use a different server log path:

```bash
cd monitoring
MEGHACONNECT_LOG_DIR=/your/api/log/path docker compose up -d promtail
```

View logs in Grafana:

1. Open Grafana.
2. Go to Explore.
3. Select datasource `Loki`.
4. Query examples:

```logql
{job="meghaconnect-api"}
{job="meghaconnect-api", level="ERROR"}
{job="meghaconnect-api", requestId="YOUR_REQUEST_ID"}
```

## Spring Boot JSON Logging

Spring Boot now writes structured JSON logs through Logback.

Important fields:

- `timestamp`
- `level`
- `logger`
- `thread`
- `message`
- `requestId`
- `application`
- `exception`

`RequestIdFilter` already sets `X-Request-Id` and stores it in SLF4J MDC. Promtail extracts `requestId`, `level`, and `logger` as Loki labels.

UAT log file:

```text
/opt/meghaconnect/logs/meghaconnect-api.log
```

## Nginx Routes

`deployment/nginx/meghaconnect.conf` now includes:

- `/grafana/` -> `http://127.0.0.1:3000/`
- `/logs/` -> `http://127.0.0.1:3100/`
- `/sonar/` -> `http://127.0.0.1:9000/sonar/`

When serving Grafana under `/grafana/`, start compose with:

```bash
cd monitoring
GRAFANA_ROOT_URL=https://meghaconnect.cloud/grafana/ \
GRAFANA_SERVE_FROM_SUB_PATH=true \
GRAFANA_ADMIN_PASSWORD='change-this-before-uat' \
docker compose up -d
```

SonarQube is already configured with:

```text
SONAR_WEB_CONTEXT=/sonar
```

Validate Nginx on the server:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## SonarQube Scan

Start SonarQube first:

```bash
cd monitoring
docker compose up -d sonarqube sonarqube-db
```

Open:

```text
http://187.127.162.84:9000/sonar
```

Default login is usually `admin/admin`; change it immediately. Create a user token, then run from the repo root:

```bash
mvn -f backend/pom.xml test
sonar-scanner -Dsonar.token=$SONAR_TOKEN
```

Docker scanner alternative:

```bash
docker run --rm \
  -e SONAR_HOST_URL=http://host.docker.internal:9000/sonar \
  -e SONAR_TOKEN=$SONAR_TOKEN \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli
```

## Semgrep

Local custom rules:

```text
quality/semgrep.yml
```

Run with Docker from the repo root:

```bash
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:latest \
  semgrep scan --config quality/semgrep.yml --json --output semgrep-results.json
```

Optional registry rules:

```bash
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:latest \
  semgrep scan --config p/owasp-top-ten --config p/secrets --config quality/semgrep.yml
```

Current local status: Semgrep CLI is not installed on this Windows machine, and Docker Desktop/Linux engine was not running when the scan was attempted. Start Docker and rerun the Docker command above.

## PMD And SpotBugs

Run backend quality checks:

```bash
cd backend
mvn -Pquality verify
```

Report files:

- `backend/target/site/pmd.html`
- `backend/target/site/cpd.html`
- `backend/target/site/spotbugs.html`
- `backend/target/spotbugsXml.xml`
- `backend/target/cpd.xml`

The quality profile is report-only for now, so it does not break UAT builds while existing findings are triaged.

## Angular Lint And Build

Run:

```bash
cd frontend
npm run lint
npm run build
```

Angular production dependency audit:

```bash
npm audit --omit=dev --audit-level=high
```

Current status:

- Angular ESLint passes with `--max-warnings=0`.
- Angular build passes.
- Production npm audit has 0 vulnerabilities.
- Full npm audit still reports dev-tool vulnerabilities in Angular CLI/build transitive packages that require a breaking major upgrade path.
- Existing bundle budget warning remains: initial bundle is about `3.11 MB` against a `2 MB` warning budget.

## Flutter Analyze And Build

Run:

```bash
cd mobile
flutter analyze
flutter build apk --debug
```

Current status:

- `flutter analyze` passes with no issues.
- Debug APK build passes.
- `constant_identifier_names` is ignored for `UserRole` because enum values intentionally match backend role/JWT names.

## Verification Status

Completed successfully:

- `mvn test`
- `mvn -Pquality verify`
- `npm run lint`
- `npm run build`
- `npm audit --omit=dev --audit-level=high`
- `flutter analyze`
- `flutter build apk --debug`
- `docker compose config` from `monitoring/`

Blocked locally:

- Semgrep Docker scan, because Docker Desktop/Linux engine was not running.

Known follow-ups:

- Review and triage PMD/SpotBugs findings in generated reports.
- Decide whether to perform a major Angular CLI/build-tool upgrade to clear dev-only audit advisories.
- Optimize Angular bundle size or adjust budgets intentionally.
- Restrict public access to Prometheus, Loki, SonarQube, and Grafana before production.

## Security Notes

- Change Grafana and SonarQube default passwords immediately.
- Do not expose Prometheus `9090`, Loki `3100`, or SonarQube `9000` publicly in production.
- Prefer Nginx with SSL, authentication, VPN, or IP allowlisting.
- Keep actuator exposure limited to `health,info,metrics,prometheus`.
- Do not log OTP, Aadhaar, authorization tokens, passwords, or raw identity documents.
