# MeghaConnect Docker CI/CD Initial Analysis

Date: 2026-08-04

This is the mandatory pre-implementation analysis. No existing deployment artifact is removed or replaced by this phase. Live facts under `/etc`, `/opt`, UFW, Docker, MySQL, Certbot, and the GitHub repository settings must be confirmed on the UAT Ubuntu server because they are not accessible from this workspace.

## Existing deployment flow

The 609-line root `deploy.sh` supports `source` and `prebuilt` modes. Source mode optionally performs a fast-forward Git pull, rewrites `frontend/src/environments/environment.dev.ts`, runs `npm ci` and an Angular UAT build, builds the backend JAR with `mvn clean package -DskipTests`, backs up/replaces `/var/www/meghaconnect`, installs `/opt/meghaconnect/backend/app.jar`, creates the runtime environment file only when absent, installs/restarts a systemd service, installs host Nginx configuration, optionally invokes Certbot/UFW, and performs local/public checks. Prebuilt mode installs Angular/JAR artifacts without compiling.

The current deployment is mutable in place: frontend files and `app.jar` are replaced, while only the frontend receives timestamped backups. There is no immutable backend artifact history, deployment lock, image scan, CI-required test gate, atomic image tag, or automatic rollback. The current JAR build explicitly skips tests and must remain available only as the emergency legacy path during migration.

## Repository and builds

- Backend: Maven, Java 17 target, Spring Boot 2.7.18, Spring MVC, Flyway, MySQL/Hikari, optional Redis, Actuator/Micrometer. The executable artifact is `backend/target/meghaconnect-1.0.0-SNAPSHOT.jar` (resolved by the legacy script using a wildcard).
- Frontend: Angular 19.2, Node-compatible lockfile, `npm ci`, ESLint, Karma/Jasmine, UAT and production build configurations. Angular output is under `frontend/dist/frontend/browser` for the application builder.
- Frontend API configuration: production already uses relative `/api/v1`; UAT currently uses `https://meghaconnect.cloud/api/v1`. A reusable container should use relative `/api/v1` for UAT and production.
- Branch state/settings: the local branch and remote branch-protection state must be captured before workflow activation. The required intended policy is PR CI on all protected branches, image publication only from the approved UAT branch/tags, and deployment only through the protected `uat` GitHub Environment. Repository-side branch protection cannot be inferred from source files.
- The worktree already contains the in-progress monitoring implementation and unrelated untracked enrollment utility files. Docker work must preserve them and avoid conflating unrelated files.

## Runtime topology and server paths

Current repository artifacts describe Ubuntu, host Nginx/Certbot, and a systemd backend service:

- Application root: `/opt/meghaconnect`
- Backend JAR: `/opt/meghaconnect/backend/app.jar`
- Runtime environment: `/etc/meghaconnect/meghaconnect-api.env`, mode intended as `0600`
- Frontend root: `/var/www/meghaconnect`
- Frontend backups: `/var/www/meghaconnect-backups`
- Uploads: `/opt/meghaconnect/uploads/dev` and `/opt/meghaconnect/uploads/uat`
- Logs: `/opt/meghaconnect/logs`
- Service: `/etc/systemd/system/meghaconnect-api.service`, user/group `meghaconnect`
- Nginx site: `/etc/nginx/sites-available/meghaconnect` and symlink under `sites-enabled`
- Certificates: `/etc/letsencrypt/live/meghaconnect.cloud/` when issued

The backend application port is normally `8080`, bound to loopback in the host deployment. The monitoring implementation uses a separate management port `9091`. `deploy.sh` and one legacy Nginx health route still reference `/api/actuator/health` on the application port and require migration-aware correction without deleting the legacy path prematurely.

## Persistence and required Docker volumes

Container recreation must not affect:

| Data | Current/target host path | Container path | Type |
|---|---|---|---|
| Uploaded documents/photos | `/opt/meghaconnect/uploads` | `/opt/meghaconnect/uploads` | Bind mount |
| Application logs | `/opt/meghaconnect/logs` | `/opt/meghaconnect/logs` | Bind mount, if file logging retained |
| Deployment state/history | `/opt/meghaconnect/deploy` | Host-only | Directory, root/deployer controlled |
| Redis data | New named volume if persistence approved | `/data` | Named volume |
| Prometheus TSDB | Existing `prometheus-data` | `/prometheus` | Named volume |
| Grafana state | Existing `grafana-data` | `/var/lib/grafana` | Named volume |
| Grafana provisioning | Repository deployment directory | `/etc/grafana/provisioning` | Read-only bind mount |

The non-root backend UID/GID must be able to read/write upload/log bind mounts before container cutover. MySQL data remains outside the new Compose project in the pilot.

## Environment contract

The existing `/etc/meghaconnect/meghaconnect-api.env` must be reused, never overwritten. Required/expected variables include:

- `SPRING_PROFILES_ACTIVE=uat`, `SERVER_PORT=8080`
- `MANAGEMENT_PORT=9091`, `MANAGEMENT_ADDRESS=0.0.0.0` inside the internal container network, and `MONITORING_BEARER_TOKEN`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST=redis`, `REDIS_PORT=6379`, `REDIS_PASSWORD`, `REDIS_CACHE_ENABLED`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`
- `FILE_UPLOAD_ROOT=/opt/meghaconnect/uploads/uat`, `LOGGING_PATH=/opt/meghaconnect/logs`
- `OLLAMA_BASE_URL=http://host.docker.internal:11434`, controlled Ollama model/timeout variables
- Face provider URL/key/client/app variables using the exact existing Spring property names
- SMS and other external-provider secrets already consumed by the application

The live environment file must be audited on the server without printing secret values. Docker Compose receives it through `env_file`; runtime secrets must not be GitHub secrets unless the deployment workflow actually needs them.

## Current Nginx routes

- `/` serves the Angular SPA with fallback to `index.html`.
- `/api/` proxies to the backend and preserves Authorization/forwarded/request-ID headers.
- `/api/v1/visitor-form-extraction/` has 30-second connect and 420-second send/read/client timeouts with buffering disabled.
- `/health` or the older `/api/actuator/health` proxies to Actuator health.
- `/grafana/` proxies to loopback Grafana; `/logs/` and `/sonar/` exist in the broader observability configuration.
- Host Nginx/Certbot remains authoritative for ports 80/443 and TLS during the pilot.

The Docker cutover should change only upstreams: frontend to `127.0.0.1:8081`, API to `127.0.0.1:8080`, health to the locally bound management port, and Grafana to `127.0.0.1:3000`. Configuration must be backed up and pass `nginx -t` before reload.

## External dependencies

- MySQL is currently selected through `DB_HOST`; repository defaults are local, but the live UAT location is unknown. It stays in place until backup/topology verification.
- Redis is optional in existing profiles and will become an internal Compose service only after its password/persistence/cache behavior is confirmed.
- Ollama remains on the host. Linux container access requires `host.docker.internal:host-gateway`, and Ollama must listen on an interface reachable from the Docker bridge without public exposure.
- Face, SMS, EPIC and other external APIs remain external and use server-held credentials.
- Host Nginx/Certbot remain outside Docker.
- Prometheus/Grafana already have Docker-oriented provisioning; they should be incorporated without losing named volumes or weakening the private Actuator boundary.

## Mandatory impact report

1. Existing deployment flow: source/prebuilt build-and-copy deployment, systemd restart, Nginx/Certbot/UFW setup, then best-effort health validation; no transactional rollback.
2. Current server paths: `/opt/meghaconnect`, `/var/www/meghaconnect`, `/var/www/meghaconnect-backups`, `/etc/meghaconnect`, `/etc/systemd/system`, `/etc/nginx`, and `/etc/letsencrypt`.
3. Persistent data paths: uploads, logs, environment/secrets, Prometheus/Grafana volumes, optional Redis data, and deployment history.
4. Required Docker volumes: upload/log bind mounts; Prometheus/Grafana named volumes; optional Redis named volume; read-only provisioning/config mounts.
5. Required environment variables: application/profile/ports, database, Redis, JWT/CORS, storage/logging, management token, Ollama, Face/SMS/provider configuration. No value is embedded in images/Compose.
6. Current Nginx routes: SPA, `/api/`, OCR long-timeout route, health, Grafana, logs, and Sonar.
7. External dependencies: current MySQL, host Ollama, host Nginx/Certbot, external Face/SMS/EPIC services, GHCR, and GitHub Actions/SSH.
8. Proposed image names: `ghcr.io/${GHCR_OWNER}/meghaconnect-backend:sha-<full-or-short-sha>` and `ghcr.io/${GHCR_OWNER}/meghaconnect-frontend:sha-<same-sha>`, optionally with `uat-latest` as a non-deployment alias.
9. Proposed Docker network: dedicated bridge `meghaconnect-internal`; only frontend `127.0.0.1:8081`, backend `127.0.0.1:8080`, management `127.0.0.1:9091`, Prometheus `127.0.0.1:9090`, and Grafana `127.0.0.1:3000` are host-bound as required.
10. Rollback strategy: persist the previous immutable `IMAGE_TAG`, deploy the requested SHA under `flock`, verify container/internal/public health, restore the prior tag and redeploy on failure. Never downgrade Flyway automatically.
11. Downtime risk: initial port/Nginx switch has brief reload-level risk; backend startup/Flyway and host-port conflicts are larger risks. Stage containers on alternate ports first and retain systemd/JAR/frontend backups until repeated UAT success.
12. Migration strategy: build locally/CI, run alternate-port containers, verify persistence/integrations, switch host Nginx, stop but retain systemd, enable manual protected GitHub deployment, then automate only after several successful releases.

## Security and operational risks

- Docker-group membership is effectively root-equivalent. Prefer a root-owned constrained deployment wrapper if organizational policy requires stronger separation.
- A containerized backend cannot use `127.0.0.1` for host MySQL/Ollama. Explicit private routing and host service binding/firewall rules are required.
- Flyway rollback is not provided by image rollback. Backward-incompatible migrations require explicit approval, backup, and expand/contract rollout.
- Redis authentication must be configured consistently for the server and application; a plaintext password must not appear in Compose or command-line process listings.
- Read-only root filesystems need writable `tmpfs` paths for Java/temp, Nginx cache/PID, and application converters.
- Current live upload ownership, disk capacity, OS/CPU architecture, Docker availability, SSL renewal, MySQL backup state, and firewall rules remain server-verification gates.
- Existing monitoring worktree changes must be preserved and committed coherently before CI workflows are relied upon.

## Gate conclusion

The proposed pilot architecture is compatible with the repository, provided MySQL/Ollama reachability and persistent-directory permissions are confirmed on UAT. The legacy `deploy.sh`, JAR, systemd unit, web root, and Certbot configuration must remain intact through staged verification. Task 1 may begin only after this report is accepted as the implementation baseline.
