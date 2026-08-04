# MeghaConnect Monitoring Operations Guide

## Architecture and access

`MeghaConnect -> Actuator/Micrometer -> Prometheus -> Grafana`. Node Exporter, mysqld_exporter, and optional Redis exporter feed Prometheus directly. Grafana is never a metric source.

The citizen API remains on `127.0.0.1:8080`. Management defaults to `127.0.0.1:9091` at `/actuator`. If Docker Prometheus scrapes through `host.docker.internal`, set `MANAGEMENT_ADDRESS` to a private host interface reachable only from the Docker bridge and enforce a host firewall rule. Never proxy port 9091 through the citizen Nginx `/api/` location.

Health/liveness/readiness remain unauthenticated for local infrastructure. `/actuator/prometheus` and `/actuator/metrics/**` require `Authorization: Bearer ...`. Store the same high-entropy token in the application environment (`MONITORING_BEARER_TOKEN`) and a root-readable file referenced by `MONITORING_BEARER_TOKEN_FILE`. Never commit it.

Grafana is bound to loopback and served through HTTPS Nginx `/grafana/`. Anonymous access and signup are disabled. Set `GRAFANA_ADMIN_PASSWORD`; no default fallback exists. Restrict Nginx access using the approved VPN, SSO/auth gateway, or administrator IP ranges.

## Actuator endpoint catalog

| Endpoint | Purpose | Access |
|---|---|---|
| `/actuator/health` | Aggregate machine health | Private infrastructure; details hidden |
| `/actuator/health/liveness` | Process liveness | Private load balancer/orchestrator |
| `/actuator/health/readiness` | Dependency readiness | Private load balancer/orchestrator |
| `/actuator/info` | Non-secret build/application info | Private |
| `/actuator/prometheus` | Prometheus scrape | Bearer token + private network |
| `/actuator/metrics` | Diagnostic meter list | Bearer token + private network |

No `env`, `beans`, `configprops`, `heapdump`, `threaddump`, `mappings`, or `shutdown` endpoint is exposed.

## Metric catalog

| Metric family | Unit | Bounded tags | Source | Dashboard | Privacy |
|---|---|---|---|---|---|
| `http_server_requests_seconds_*` | seconds/count | method, route template, status, outcome, exception | Spring MVC/Micrometer | Overview/API | Technical |
| `jvm_memory_*`, `jvm_gc_*`, `jvm_threads_*` | bytes/seconds/count | JVM pool/area/action | JVM binder | JVM & Server | Technical |
| `process_*`, `system_*` | ratio/seconds/count | application/environment | JVM/process binders | JVM & Server | Technical |
| `hikaricp_connections_*` | count/seconds | pool | Hikari binder | Database | Technical |
| `meghaconnect_face_operation_*` | seconds/count | provider, operation, result | Face service | Integrations | Technical; no images/IDs |
| `meghaconnect_face_result_total` | count | operation, result | Face service | Integrations | Technical |
| `meghaconnect_form_extraction_*` | seconds/count | provider, controlled model, result | Form extraction | Integrations | Technical; no form data |
| `meghaconnect_external_api_errors_total` | count | provider, operation, result | External client wrapper | Integrations/alerts | Technical |
| `meghaconnect_operation_total` | count | fixed operation, category, result | Monitored service aspect | Integrations | Technical; no arguments |
| `meghaconnect_operation_duration_seconds_*` | seconds/count | fixed operation, category, result | Monitored service aspect | Integrations | Technical; no arguments |
| `meghaconnect_db_operation_*` | seconds/count | fixed operation, result | Monitored service aspect | Database | Technical; no SQL/values |
| `meghaconnect_executor_*` | threads/tasks/queue | executor | Executor binder | JVM & Server | Technical |
| `node_*` | bytes/seconds/count | host/device/mount | Node Exporter | JVM & Server | Infrastructure |
| `mysql_*` | count/bytes/seconds | instance/schema where bounded | mysqld_exporter | Database | Infrastructure; no SQL |
| `redis_*` | count/bytes/seconds | instance/db | Redis exporter | Redis & Cache | Infrastructure; no keys |

Forbidden metric labels include usernames, user/citizen/visitor/department IDs, mobile/EPIC values, request IDs, JWTs, IP addresses, raw URLs, SQL, exception messages, images, OTPs, and CAPTCHA data.

Bounded operation values currently include `visitor_registration`, `appointment_creation`, `appointment_search`, `appointment_lookup`, `citizen_lookup_by_phone`, `citizen_profile_lookup`, `public_identification`, `face_search`, `face_enrollment`, `face_verify`, `face_compare`, `face_delete`, `ocr_form_extraction`, `otp_generation`, `otp_validation`, `captcha_generation`, `captcha_validation`, `reference_data_lookup`, `scheme_list_load`, `department_user_load`, `file_upload`, `sms_provider_call`, and `public_darbar_scheduler`.

Hikari metrics describe pool behavior; they do not provide per-SQL timing. Use bounded service/repository operation timers for application operations. MySQL slow-query logs and `performance_schema` are separate operational sources and must be reviewed with `EXPLAIN`; use Loki/ELK/OpenSearch if query-log visualization is approved.

## Installation

1. Install Docker Engine/Compose on the monitoring host. Create a least-privilege MySQL exporter user limited to performance/status reads and store its client configuration outside Git with mode `0600`.
2. Generate a random monitoring bearer token and root-readable token file. Configure the application environment and restart the service.
3. Set `MONITORING_BEARER_TOKEN_FILE`, `MYSQL_EXPORTER_CONFIG_FILE`, `GRAFANA_ADMIN_PASSWORD`, and an HTTPS `GRAFANA_ROOT_URL` in a protected Compose environment file.
4. From `monitoring/`, validate with `docker compose config`, then run `docker compose up -d`. Add `--profile redis` only when Redis is enabled.
5. Configure the firewall so 9090, 9091, 9100, 9104, 9121, and 3000 are not Internet-accessible. Expose only HTTPS Nginx/Grafana to the approved administrator network.
6. In Prometheus verify `meghaconnect-api`, `node`, `mysql`, `prometheus`, and optionally `redis` are UP. In Grafana verify the provisioned Prometheus source and six dashboards.

After deployment, administrators monitor at `https://YOUR_DOMAIN/grafana/`. The application URL is not used as the metrics UI. Prometheus stays at loopback `http://127.0.0.1:9090`, while Actuator stays on the private management listener. Create individual Grafana accounts or connect the approved SSO provider; do not share the bootstrap administrator account.

Required application service environment:

```text
SPRING_PROFILES_ACTIVE=prod
MANAGEMENT_PORT=9091
MANAGEMENT_ADDRESS=127.0.0.1
MONITORING_BEARER_TOKEN=<same random value stored in monitoring.token>
```

If Prometheus runs in Docker and the application runs directly on Linux, loopback is not reachable through `host.docker.internal`. Bind the management listener to the Docker bridge/private server address and allow port 9091 only from the Docker bridge subnet. Never add a public Nginx route for `/actuator/prometheus`.

For MySQL, enable `performance_schema` and a policy-approved slow-query log in UAT first. Start `long_query_time` at 2 seconds and tune from evidence. Restrict and rotate the log; do not put SQL text in Prometheus labels or dashboard panels.

## Alerts and pilot targets

Provisioned rules cover application down, 5xx ratio >5%, P95 >2s, heap >80%, Hikari utilization >80%, sustained pending connections, host memory >90%, and disk >90%. Route alerts through the organization’s approved contact point after deployment; tune over a two-week pilot. Do not alert on individual failures.

Initial measurement targets—not guarantees—are average normal API latency <500 ms, P95 <1.5 s, P99 <3 s, error rate <1%, pending DB connections 0, heap <80%, and sustained CPU <80%. Face/OCR use separate provider-specific SLOs.

## Retention, backup, and restore

Prometheus retention starts at 15 days. Reserve 10-20 GB, measure actual bytes/day and series count for a week, and expand toward 30 days only with adequate headroom. Grafana dashboard JSON and provisioning are already source-controlled. Back up Grafana and Prometheus named volumes using the approved volume snapshot process while services are quiesced; test restoration quarterly. Secrets are restored from the secrets manager, never from source control.

## Troubleshooting

- API target DOWN: confirm management listener/address, token equality/newline handling, Docker host gateway, and firewall. Use `curl -H "Authorization: Bearer $TOKEN" http://PRIVATE_HOST:9091/actuator/prometheus` locally without printing the token.
- HTTP 401: token is missing/mismatched or the application was not restarted after rotation.
- Missing percentiles: generate traffic and confirm histogram buckets exist; summaries/percentiles are not interchangeable across instances.
- Missing Hikari metrics: verify datasource startup and inspect `/actuator/metrics/hikaricp.connections.active` with authorization.
- MySQL exporter DOWN: verify the exporter-only account, TLS/host grants, and protected `.my.cnf` syntax.
- High cardinality: inspect top series/labels, find raw URI or uncontrolled model/exception values, then remove the label before increasing storage.
- Dashboard empty: check time range, variable selections, Prometheus data source health, job labels, and exact metric names.

## Load-test baseline procedure

Use k6/JMeter/Gatling from a non-production runner against UAT. Test login, reference data, appointment list, citizen lookup, visitor registration, and user list. Mock Face and OCR for the main concurrency test; test OCR separately. Never load-test real SMS or biometric providers without approval.

Record build/version, dataset, concurrent users, duration, request rate, average/P95/P99, errors, CPU, memory, Hikari active/pending, Redis evictions/hit rate, and mock latency. The repository has no trustworthy executed baseline yet; populate the template below only from a controlled run.

| Scenario | VUs | Duration | RPS | Avg | P95 | P99 | Error % | CPU | Heap | DB pending | Notes |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Pending pilot run | | | | | | | | | | | |

## Scale-out recommendations

For multiple backend instances, scrape each instance, aggregate dashboard queries by application/environment, and keep instance available for diagnosis. Move Prometheus/Grafana to a dedicated monitoring network, add Alertmanager and durable remote storage when retention/HA requires it, use centralized secrets/SSO, and capacity-plan from measured active-series and ingestion rates.
