#!/usr/bin/env bash
set -euo pipefail

: "${MONITORING_BEARER_TOKEN_FILE:?Set MONITORING_BEARER_TOKEN_FILE}"
: "${GRAFANA_URL:?Set GRAFANA_URL, for example https://your-domain/grafana/}"

MANAGEMENT_URL="${MANAGEMENT_URL:-http://127.0.0.1:9091}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://127.0.0.1:9090}"
TOKEN="$(tr -d '\r\n' < "$MONITORING_BEARER_TOKEN_FILE")"

test -n "$TOKEN"
curl --fail --silent --show-error "$MANAGEMENT_URL/actuator/health" >/dev/null
if curl --silent --output /dev/null --write-out '%{http_code}' "$MANAGEMENT_URL/actuator/prometheus" | grep -q '^200$'; then
  echo "ERROR: unauthenticated Prometheus endpoint returned 200" >&2
  exit 1
fi
curl --fail --silent --show-error -H "Authorization: Bearer $TOKEN" \
  "$MANAGEMENT_URL/actuator/prometheus" | grep -q 'http_server_requests_seconds'
curl --fail --silent --show-error "$PROMETHEUS_URL/-/ready" >/dev/null

for job in meghaconnect-api node mysql prometheus; do
  value="$(curl --fail --silent --get "$PROMETHEUS_URL/api/v1/query" \
    --data-urlencode "query=max(up{job=\"$job\"})" | sed -n 's/.*"value":\[[^,]*,"\([^"]*\)"\].*/\1/p')"
  test "$value" = "1" || { echo "ERROR: Prometheus job $job is not UP" >&2; exit 1; }
done

curl --fail --silent --show-error "$GRAFANA_URL/api/health" | grep -q '"database"'
echo "Monitoring verification passed: Actuator secured, Prometheus ready/targets UP, Grafana healthy."
