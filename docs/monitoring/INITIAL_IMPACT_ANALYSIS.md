# MeghaConnect Monitoring Initial Impact Analysis

Date: 2026-08-04

This report records the repository state before the production monitoring implementation. It is based on source and deployment artifacts; live server, firewall, target, disk, and operating-system state cannot be proven from this workstation alone.

## Existing topology and implementation

- Backend: Java 17, Spring Boot 2.7.18, Spring MVC (`spring-boot-starter-web`), Spring Security with stateless JWT, Spring Data JPA, MySQL Connector/J, and HikariCP.
- Runtime: application port `8080`; dev and production profiles bind the application to `127.0.0.1` by default and Nginx proxies `/api/` to it. Deployment documentation describes a Linux/systemd application service and Docker-based supporting services.
- Profiles: base `application.properties` defaults to `dev`; repository profiles are `dev`, `uat`, and `prod`.
- Actuator: `spring-boot-starter-actuator` and `micrometer-registry-prometheus` already exist and use Spring Boot dependency management.
- Existing Actuator paths: dev/prod configure the base path `/api/actuator` and expose `health`, `info`, `metrics`, and `prometheus`. Health probes are enabled. The legacy properties template exposes only `health`, `info`, and `metrics`.
- Existing security: both `/actuator/**` and `/api/actuator/**` health, info, metrics, and Prometheus paths are currently `permitAll`. This publicly exposes metrics whenever Nginx proxies `/api/`. Dev health details/components are configured as `always`.
- Existing metrics: Spring Boot provides HTTP server, JVM, process, system, disk-space, logging, Hikari, datasource, and uptime meters when their binders are available. Face recognition has request/success/failure/result counters but no provider-call timer. Form extraction has duration, success, and manual-review meters. Names and result classification are not yet consistent with the target catalog.
- Health indicators: no custom `HealthIndicator` implementation was found. Standard database/disk and optional Redis health contributors are used.
- Database: MySQL with HikariCP. Pool sizes are profile-controlled. Hikari provides pool metrics, not SQL statement timings. No database-operation timers or MySQL exporter configuration was found.
- Redis: dependency and optional cache configuration exist. Redis auto-configuration is disabled in dev/prod unless deployment configuration changes; no Redis exporter exists.
- Async/scheduling: one `ThreadPoolTaskExecutor` (core 4, max 16, queue 100) serves face enrollment, face search futures, and AI document work. A Public Darbar scheduler exists. No executor or scheduler meters were found.
- Third-party clients: OkHttp is used for DeepFace and form-extraction providers; RestTemplate is used for SMS, EPIC verification, and Ollama. Metrics are partial and no common bounded-tag external-call convention exists.
- Correlation: request ID filter/advice and MDC logging already exist. Request IDs are not metric labels, which is correct.
- Audit/logging: application audit services and rolling application logs exist. Loki/Promtail configuration exists separately; metrics must not replace audit logs.
- Frontend: Angular has admin routes and dashboards but no technical monitoring component. Grafana is the intended UI; no Angular polling page should be added. The existing roles include `SUPER_ADMIN` but not `TECHNICAL_ADMIN`.
- Monitoring stack: Docker Compose defines Prometheus, Grafana, Loki, and Promtail. Prometheus scrapes `host.docker.internal:8080/api/actuator/prometheus` every 15 seconds with 15-day retention. Grafana has provisioned Prometheus/Loki data sources, but no dashboard provisioning or dashboards were found.
- Infrastructure gaps: no Node Exporter, mysqld_exporter, Redis exporter, Alertmanager, Prometheus alert rules, or live target verification artifacts. Prometheus/Grafana/Loki/Sonar ports are host-published; Prometheus uses `latest`, Grafana uses `latest`, and Grafana has an insecure default-password fallback. Nginx proxies Grafana but does not add an independent authentication control. Live firewall, disk capacity, MySQL topology, and host OS version require deployment-host verification.

## Mandatory impact report

1. Existing Actuator endpoints: `/api/actuator/health`, health probe children, `/api/actuator/info`, `/api/actuator/metrics`, and `/api/actuator/prometheus` in dev/prod.
2. Existing Micrometer metrics: automatic HTTP/JVM/process/system/log/Hikari/datasource metrics plus partial Face and form-extraction custom meters.
3. Missing dependencies: none for Actuator/Prometheus. No application dependency is required for Node/MySQL/Redis exporters because they are separate processes.
4. Current endpoint security: unsafe. Metrics, Prometheus, info, and detailed health are explicitly anonymous and reachable beneath the publicly proxied `/api/` prefix.
5. Current application port: `8080`, normally loopback-bound in dev/prod profile YAML.
6. Proposed scrape endpoint: private `http://host.docker.internal:9091/actuator/prometheus`, using a dedicated loopback management port. Container-to-host reachability must be verified on the deployment host; otherwise use a private host address/firewall rule.
7. Proposed Grafana access: HTTPS at `/grafana/` through Nginx, authenticated by Grafana, anonymous access and signup disabled, limited to technical administrators/VPN or IP allowlist.
8. Required exporters: Node Exporter and mysqld_exporter; Redis exporter only when Redis is enabled. Exporters must bind privately.
9. Required custom metrics: bounded operation timers/counters for critical business operations, DB operations, Face/OCR/Ollama/SMS calls, outcomes/timeouts, and the shared executor. No PII or unbounded tags.
10. Security risks: anonymous Actuator metrics/detailed health, public host port bindings, default credential fallbacks, mutable container tags, public Grafana proxy without network restriction, and secrets potentially passed directly through environment variables. Metrics must never contain citizen/biometric/request payload data.
11. Expected storage: highly workload/cardinality dependent. For the pilot, reserve at least 10-20 GB for 15 days of Prometheus data and measure actual bytes/day after one week; reserve separate capacity for Grafana and logs. This is a planning estimate, not a measured requirement.
12. Recommended retention: 15 days initially, expandable to 30 days only after measuring ingestion/cardinality and confirming disk headroom. Configure alerts for disk thresholds before extension.

## Expected impact

- A separate management port changes the Prometheus target but not citizen API URLs. Nginx must not proxy the management port.
- Securing metrics may break the current anonymous scrape until Prometheus is moved to the private management endpoint; deployment and configuration must be applied together.
- HTTP histograms increase Prometheus series count. Route-template tags and bounded custom tags control cardinality.
- Exporters add small host/container CPU and memory overhead and require least-privilege credentials/network rules.
- Custom timers add negligible request overhead but must wrap operations without changing exceptions or business results.
- Dashboard and alert accuracy depends on actual exported metric names in Spring Boot 2.7/Micrometer 1.9 and exporter versions; queries must be validated against a running target.

## Implementation gates

Each implementation group will be compiled and tested before proceeding. Live infrastructure acceptance items (targets UP, Grafana login/data source, firewall, HTTPS, disk capacity, and load baseline) remain deployment-host gates and cannot be truthfully claimed from repository-only verification.
