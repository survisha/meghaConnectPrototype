# MeghaConnect Monitoring

This stack runs Prometheus, Grafana, Loki, Promtail, and SonarQube for local/UAT monitoring, logging, and code-quality review.

For the full setup guide, see `../docs/QUALITY_OBSERVABILITY_SETUP.md`.

## Backend Endpoints

The backend dev profile exposes these endpoints under the configured actuator base path:

- Health: `https://meghaconnect.cloud/api/actuator/health`
- Prometheus: `https://meghaconnect.cloud/api/actuator/prometheus`
- Metrics list: `https://meghaconnect.cloud/api/actuator/metrics`

The raw Spring Boot service target used by Prometheus is:

`http://host.docker.internal:8080/api/actuator/prometheus`

On Ubuntu servers, `host.docker.internal` is mapped through Docker's `host-gateway` setting in `docker-compose.yml`. If it still does not resolve, update `prometheus.yml` to use `172.17.0.1:8080` or the server internal IP.

## Start

```bash
cd monitoring
docker compose up -d
```

Check containers:

```bash
docker ps
```

Prometheus:

`http://187.127.162.84:9090`

Grafana:

`http://187.127.162.84:3000`

Loki API:

`http://187.127.162.84:3100`

SonarQube:

`http://187.127.162.84:9000/sonar`

Default UAT login is `admin/admin` unless `GRAFANA_ADMIN_PASSWORD` is set. Change the password immediately after first login, or set it before start:

```bash
GRAFANA_ADMIN_PASSWORD='change-this-before-uat' docker compose up -d
```

## Grafana Dashboard And Logs

The Prometheus datasource is provisioned automatically as `Prometheus`.
The Loki datasource is provisioned automatically as `Loki`.

Import a standard Spring Boot Micrometer/JVM dashboard from Grafana dashboards, such as `JVM (Micrometer)` or `Spring Boot Micrometer`. Select the `Prometheus` datasource during import.

Useful Prometheus metrics include:

- JVM memory: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- CPU: `process_cpu_usage`, `system_cpu_usage`
- Threads: `jvm_threads_live_threads`, `jvm_threads_daemon_threads`
- HTTP requests: `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, `http_server_requests_seconds_max`
- DB pool: `hikaricp_connections_active`, `hikaricp_connections_idle`, `hikaricp_connections_pending`
- GC: `jvm_gc_pause_seconds_count`, `jvm_gc_pause_seconds_sum`, `jvm_gc_pause_seconds_max`
- Application scrape status: `up{job="meghaconnect-api"}`

Use Grafana Explore with datasource `Loki` for log queries:

```logql
{job="meghaconnect-api"}
{job="meghaconnect-api", level="ERROR"}
```

## Nginx Subpath

Use `nginx-grafana.conf` or `deployment/nginx/meghaconnect.conf` if the tools should be available at:

- `https://meghaconnect.cloud/grafana/`
- `https://meghaconnect.cloud/logs/`
- `https://meghaconnect.cloud/sonar/`

Set these values before starting Grafana:

```bash
GRAFANA_ROOT_URL=https://meghaconnect.cloud/grafana/
GRAFANA_SERVE_FROM_SUB_PATH=true
docker compose up -d
```

## Firewall And Security

For UAT only:

```bash
sudo ufw allow 3000
sudo ufw allow 9090
```

For production, do not expose Prometheus port `9090` publicly. Prefer exposing only Grafana through Nginx with SSL and authentication, or keep both services private behind VPN/IP allowlisting. Keep actuator exposure limited to `health,info,metrics,prometheus`.
