# Meghalaya Connect Docker CI/CD deployment

This is the production-shaped UAT migration runbook. The current JAR, static frontend, systemd units, enabled Nginx site, MySQL, Ollama and certificates remain untouched until the container stack passes UAT and an operator performs the explicit cutover.

## Resulting topology

Internet traffic terminates at the existing host Nginx and certificate. Nginx proxies the SPA to the non-root frontend container on `127.0.0.1:8081`, `/api/` to the non-root Spring Boot container on `127.0.0.1:8080`, `/management/health` to its separate management port on `127.0.0.1:9091`, and `/grafana/` to Grafana on `127.0.0.1:3000`. MySQL and Ollama stay on the host. Redis, Prometheus, Grafana, Loki, Promtail, node-exporter, mysqld-exporter and redis-exporter run on the private Compose network. No database, Redis, logs, metrics or dashboard port is publicly bound.

The public URL is `https://meghaconnect.cloud`. Once the candidate Nginx site is enabled, that single URL serves the UI and relative `/api/v1` calls. The authenticated monitoring UI is `https://meghaconnect.cloud/grafana/`. Prometheus itself remains loopback-only; use an SSH tunnel when direct access is needed.

## GitHub configuration

Create a protected GitHub Environment named `uat`, require reviewers, and restrict its deployment branch policy to `phase2-new-release`. Add:

| Type | Name | Value |
|---|---|---|
| Environment variable | `UAT_DOMAIN` | `meghaconnect.cloud` |
| Environment secret | `UAT_SERVER_HOST` | Server DNS name or IP |
| Environment secret | `UAT_SERVER_PORT` | SSH port, normally `22` |
| Environment secret | `UAT_DEPLOY_USER` | `meghadeploy` |
| Environment secret | `UAT_SSH_PRIVATE_KEY` | Private half of a dedicated deploy key |
| Environment secret | `UAT_SSH_KNOWN_HOSTS` | Pinned `ssh-keyscan` output verified out of band |

Set repository variables `GHCR_REGISTRY=ghcr.io`, `BACKEND_IMAGE=ghcr.io/<lowercase-owner>/meghaconnect-backend`, and `FRONTEND_IMAGE=ghcr.io/<lowercase-owner>/meghaconnect-frontend` for operator visibility. The publish workflow derives these coordinates from the repository owner to prevent drift; the server `.env` uses `GHCR_REGISTRY` and `GHCR_OWNER`.

Do not store application credentials in GitHub variables, images, Compose YAML or the repository. GitHub's generated `GITHUB_TOKEN` publishes to GHCR. If the package is private, authenticate the server once with a read-only classic PAT or fine-grained token that can read packages:

```bash
sudo -iu meghadeploy docker login ghcr.io
```

The deploy SSH key must only belong to `meghadeploy`; disable password authentication after verifying key access. Docker-group membership is intentionally limited to that account but is root-equivalent. The deploy account needs passwordless sudo only for Nginx validation/reload during cutover, for example `/usr/sbin/nginx -t` and `/usr/bin/systemctl reload nginx`; it does not need general passwordless sudo.

## One-time server bootstrap

Copy the repository to a temporary server directory, review the script, then run:

```bash
sudo DOMAIN=meghaconnect.cloud WWW_DOMAIN=www.meghaconnect.cloud ./deploy/bootstrap-server.sh
```

The script installs Docker from Docker's official Ubuntu repository if needed; creates the restricted deploy account, directories and shared application group; copies the Compose, deployment and monitoring configuration; installs an **inactive** Nginx candidate; and preserves existing secret files, enabled Nginx configuration and systemd services.

Populate `/opt/meghaconnect/deploy/.env`. For parallel Stage 1 validation, use non-conflicting ports:

```dotenv
BACKEND_HOST_PORT=18080
FRONTEND_HOST_PORT=18081
MANAGEMENT_HOST_PORT=19091
VERIFY_PUBLIC=false
REQUIRE_MIGRATION_APPROVAL=true
```

Populate `/etc/meghaconnect/meghaconnect-api.env` from its generated template and set mode `0640`, owner `root`, group `meghaconnect`. Set unique strong values for the database, JWT, actuator and Redis credentials. Also create:

```bash
sudo install -m 0640 -o root -g meghaconnect /dev/null /etc/meghaconnect/secrets/grafana-admin-password
sudo install -m 0640 -o root -g meghaconnect /dev/null /etc/meghaconnect/secrets/monitoring-bearer-token
sudo install -m 0640 -o root -g meghaconnect /dev/null /etc/meghaconnect/secrets/mysql-exporter.cnf
```

Write the secret values without a trailing blank line. The MySQL exporter file uses standard client syntax:

```ini
[client]
user=prometheus_exporter
password=replace-me
host=host.docker.internal
port=3306
```

Grant that database account only the minimum metrics permissions. Ensure host MySQL and Ollama accept connections from Docker's bridge gateway, while the host firewall continues to deny them externally.

## Build and release flow

Pull requests and protected branch pushes execute `.github/workflows/ci.yml`: Maven verification, Angular lint/test/UAT build, secret scanning and filesystem vulnerability scanning. A push to `phase2-new-release`, a `v*` tag, or a manual invocation executes `.github/workflows/publish-images.yml`. It repeats required tests, creates OCI metadata, builds Linux AMD64 images, blocks on high/critical fixed vulnerabilities, and publishes both images with the immutable tag `sha-<full commit>`.

Run `Deploy UAT` manually with that exact immutable tag. Check `approve_migrations` only after reviewing Flyway changes and confirming the database backup/restore policy. The protected `uat` environment supplies the human approval gate.

The server deployment is serialized with `flock`; it pulls exact tags, starts the stack, waits for Redis/backend/frontend health, tests internal routes and Nginx configuration, and records JSON history under `/opt/meghaconnect/deploy/history`. Failure automatically restores the previously recorded immutable tag. Images are deliberately not pruned so rollback remains available.

## Staged migration and cutover

1. Keep the legacy systemd backend and current Nginx site running. Deploy containers on `18080`, `18081` and `19091` with `VERIFY_PUBLIC=false`.
2. Verify `curl -f http://127.0.0.1:18081/health`, `curl -f http://127.0.0.1:19091/actuator/health/readiness`, authentication, upload/download persistence, OCR, host MySQL, Ollama, Redis, Prometheus targets, Grafana dashboards and alert rules.
3. Take a tested database backup. Confirm Flyway compatibility; schema rollback is a separate DBA decision and is never attempted automatically.
4. Stop the legacy application service only when the change window begins. Change ports in `/opt/meghaconnect/deploy/.env` to `8080`, `8081`, `9091`, set `VERIFY_PUBLIC=true`, and redeploy the same immutable tag.
5. Compare `/etc/nginx/sites-available/meghaconnect-docker-candidate` with the enabled site. Atomically switch the site symlink, run `sudo nginx -t`, then `sudo systemctl reload nginx`. Do not edit or reissue Certbot certificates.
6. Verify `https://meghaconnect.cloud`, a representative API call, OCR, `https://meghaconnect.cloud/management/health`, and authenticated `https://meghaconnect.cloud/grafana/`. Observe error rate, latency, JVM, host, MySQL and Redis dashboards through the agreed soak period.
7. Keep the legacy artifacts and service definition disabled but recoverable until UAT sign-off and rollback-window expiry.

## Rollback

For a failed automated deployment, inspect `/opt/meghaconnect/deploy/deployment.log`; the script already attempts the previous image tag. For a manual image rollback, run the deploy workflow using the previous `sha-...` tag. Do not use `uat-latest` for deployment.

For emergency legacy rollback: restore the previous enabled Nginx site, validate and reload Nginx, stop the Compose application containers if they occupy legacy ports, and restart the existing systemd service. If a release applied a non-backward-compatible Flyway migration, stop and follow the approved database restore/forward-fix procedure before bringing either application version online.

## Operations

Useful server commands:

```bash
cd /opt/meghaconnect/deploy
docker compose --env-file .env -f docker-compose.uat.yml ps
docker compose --env-file .env -f docker-compose.uat.yml logs --tail=200 backend
curl -f http://127.0.0.1:9091/actuator/health/readiness
ssh -L 9090:127.0.0.1:9090 meghadeploy@meghaconnect.cloud
```

Back up MySQL, `/opt/meghaconnect/uploads`, and the Redis/Grafana/Prometheus named volumes according to retention requirements. Rotate application, Redis, Grafana and monitoring credentials on a schedule, then recreate affected containers. Patch pinned base images through reviewed pull requests, rebuild under a new immutable SHA tag, scan, deploy and retain at least the last known-good images.

Acceptance is complete only after CI is green, both GHCR digests are recorded, the protected deploy succeeds, all containers are healthy, the public TLS/API/OCR flows pass, every Prometheus target is up, Grafana dashboards populate, the rollback drill succeeds, and the legacy deployment is retained until written sign-off.
