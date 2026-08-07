#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/meghaconnect/deploy}"
COMPOSE_FILE="${COMPOSE_FILE:-${DEPLOY_ROOT}/docker-compose.uat.yml}"
DEPLOY_ENV_FILE="${DEPLOY_ENV_FILE:-${DEPLOY_ROOT}/.env}"
STATE_FILE="${STATE_FILE:-${DEPLOY_ROOT}/current-image-tag}"
HISTORY_DIR="${HISTORY_DIR:-${DEPLOY_ROOT}/history}"
LOG_FILE="${LOG_FILE:-${DEPLOY_ROOT}/deployment.log}"
LOCK_FILE="${LOCK_FILE:-${DEPLOY_ROOT}/.deployment.lock}"
PUBLIC_URL="${PUBLIC_URL:-}"
VERIFY_PUBLIC="${VERIFY_PUBLIC:-}"
HEALTH_ATTEMPTS="${HEALTH_ATTEMPTS:-40}"
HEALTH_DELAY_SECONDS="${HEALTH_DELAY_SECONDS:-5}"

IMAGE_TAG=""
DEPLOY_ACTOR="manual"
DEPLOY_COMMIT="unknown"
MIGRATIONS_APPROVED="false"
PREVIOUS_TAG=""
DEPLOY_STARTED="false"
ROLLBACK_OUTCOME="not_required"
HEALTH_OUTCOME="not_started"

usage() {
  cat <<'EOF'
Usage: deploy-docker.sh --image-tag sha-<40-hex-commit> [options]
  --actor <github-user-or-operator>
  --commit <git-commit>
  --approve-migrations   Confirms migration review/backup policy was completed.
EOF
}

log() {
  local line
  line="$(date -u +'%Y-%m-%dT%H:%M:%SZ') [deploy] $*"
  echo "$line" | tee -a "$LOG_FILE"
}

die() {
  log "ERROR: $*"
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

compose() {
  docker compose --env-file "$DEPLOY_ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

read_env_value() {
  local key="$1"
  awk -F= -v key="$key" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "$DEPLOY_ENV_FILE"
}

write_image_tag() {
  local tag="$1" temporary
  temporary="$(mktemp "${DEPLOY_ROOT}/.env.XXXXXX")"
  awk -v tag="$tag" '
    BEGIN { replaced=0 }
    /^IMAGE_TAG=/ { print "IMAGE_TAG=" tag; replaced=1; next }
    { print }
    END { if (!replaced) print "IMAGE_TAG=" tag }
  ' "$DEPLOY_ENV_FILE" > "$temporary"
  chmod --reference="$DEPLOY_ENV_FILE" "$temporary"
  mv -f "$temporary" "$DEPLOY_ENV_FILE"
}

wait_for_container_health() {
  local service="$1" container="$2" attempt status
  for ((attempt=1; attempt<=HEALTH_ATTEMPTS; attempt++)); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      log "$service container is healthy"
      return 0
    fi
    if [[ "$status" == "unhealthy" || "$status" == "exited" || "$status" == "dead" ]]; then
      log "$service entered terminal status: $status"
      docker logs --tail 100 "$container" 2>&1 | tee -a "$LOG_FILE" || true
      return 1
    fi
    sleep "$HEALTH_DELAY_SECONDS"
  done
  log "$service did not become healthy within the configured timeout"
  docker logs --tail 100 "$container" 2>&1 | tee -a "$LOG_FILE" || true
  return 1
}

wait_for_url() {
  local label="$1" url="$2" attempt
  for ((attempt=1; attempt<=HEALTH_ATTEMPTS; attempt++)); do
    if curl --fail --silent --show-error "$url" >/dev/null 2>&1; then
      log "$label endpoint is ready"
      return 0
    fi
    sleep "$HEALTH_DELAY_SECONDS"
  done
  log "$label endpoint did not become ready: $url"
  return 1
}

verify_routes() {
  local frontend_port management_port
  frontend_port="$(read_env_value FRONTEND_HOST_PORT)"
  management_port="$(read_env_value MANAGEMENT_HOST_PORT)"
  frontend_port="${frontend_port:-8081}"
  management_port="${management_port:-9091}"

  curl --fail --silent --show-error "http://127.0.0.1:${frontend_port}/health" >/dev/null
  curl --fail --silent --show-error "http://127.0.0.1:${management_port}/actuator/health/readiness" >/dev/null
  wait_for_url Prometheus "http://127.0.0.1:9090/-/ready"
  wait_for_url Grafana "http://127.0.0.1:3000/api/health"
  wait_for_url Loki "http://127.0.0.1:3100/ready"

  local targets
  targets="$(curl --fail --silent --show-error 'http://127.0.0.1:9090/api/v1/targets?state=active')"
  if grep -q '"health":"down"' <<<"$targets"; then
    log "One or more active Prometheus scrape targets are down"
    return 1
  fi
  log "Prometheus, Grafana, Loki, and all active scrape targets passed"

  if command -v nginx >/dev/null 2>&1; then
    if sudo -n nginx -t >>"$LOG_FILE" 2>&1; then
      log "Nginx configuration validation passed"
    else
      return 1
    fi
  fi

  if [[ "$VERIFY_PUBLIC" == "true" ]]; then
    curl --fail --silent --show-error --location --max-time 30 "$PUBLIC_URL" >/dev/null
    log "Public HTTPS check passed: $PUBLIC_URL"
  else
    log "Public HTTPS check skipped for alternate-port staging"
  fi
}

deploy_tag() {
  local tag="$1"
  write_image_tag "$tag"
  log "Pulling immutable images for $tag"
  compose pull backend frontend
  log "Applying Compose project for $tag"
  compose up -d --remove-orphans
  wait_for_container_health backend meghaconnect-backend
  wait_for_container_health frontend meghaconnect-frontend
  wait_for_container_health redis meghaconnect-redis
  verify_routes
}

record_history() {
  local outcome="$1" timestamp history_file
  timestamp="$(date -u +'%Y%m%dT%H%M%SZ')"
  history_file="${HISTORY_DIR}/${timestamp}-${IMAGE_TAG}.json"
  cat > "$history_file" <<EOF
{
  "timestamp": "${timestamp}",
  "previousImageTag": "${PREVIOUS_TAG}",
  "newImageTag": "${IMAGE_TAG}",
  "gitCommit": "${DEPLOY_COMMIT}",
  "deployActor": "${DEPLOY_ACTOR}",
  "healthOutcome": "${HEALTH_OUTCOME}",
  "rollbackOutcome": "${ROLLBACK_OUTCOME}",
  "deploymentOutcome": "${outcome}"
}
EOF
  chmod 640 "$history_file"
}

rollback() {
  if [[ -z "$PREVIOUS_TAG" ]]; then
    ROLLBACK_OUTCOME="not_possible_no_previous_tag"
    log "No previous immutable tag is recorded; legacy systemd/JAR remains the emergency path"
    return 1
  fi
  log "Rolling back to $PREVIOUS_TAG"
  if deploy_tag "$PREVIOUS_TAG"; then
    printf '%s\n' "$PREVIOUS_TAG" > "$STATE_FILE"
    ROLLBACK_OUTCOME="success"
    log "Rollback passed"
    return 0
  fi
  ROLLBACK_OUTCOME="failure"
  log "Rollback failed; operator intervention is required"
  return 1
}

on_error() {
  local exit_code=$?
  trap - ERR
  HEALTH_OUTCOME="failure"
  if [[ "$DEPLOY_STARTED" == "true" ]]; then
    rollback || true
  fi
  record_history failure || true
  exit "$exit_code"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image-tag) IMAGE_TAG="${2:-}"; shift 2 ;;
    --actor) DEPLOY_ACTOR="${2:-}"; shift 2 ;;
    --commit) DEPLOY_COMMIT="${2:-}"; shift 2 ;;
    --approve-migrations) MIGRATIONS_APPROVED="true"; shift ;;
    --help|-h) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done

[[ "$IMAGE_TAG" =~ ^sha-[0-9a-f]{40}$ ]] || { usage >&2; exit 2; }
[[ "$DEPLOY_ACTOR" =~ ^[A-Za-z0-9_.@-]+$ ]] || die "Invalid deploy actor"
[[ "$DEPLOY_COMMIT" =~ ^(unknown|[0-9a-f]{7,40})$ ]] || die "Invalid deploy commit"

require_command docker
require_command curl
require_command flock
docker compose version >/dev/null 2>&1 || die "Docker Compose plugin is unavailable"
[[ -f "$COMPOSE_FILE" ]] || die "Compose file not found: $COMPOSE_FILE"
[[ -f "$DEPLOY_ENV_FILE" ]] || die "Deployment environment not found: $DEPLOY_ENV_FILE"
mkdir -p "$HISTORY_DIR"
touch "$LOG_FILE"

# Operational flags may be supplied by the caller; otherwise read their persisted
# values without sourcing the file (which prevents command execution from .env).
VERIFY_PUBLIC="${VERIFY_PUBLIC:-$(read_env_value VERIFY_PUBLIC)}"
VERIFY_PUBLIC="${VERIFY_PUBLIC:-true}"
REQUIRE_MIGRATION_APPROVAL="${REQUIRE_MIGRATION_APPROVAL:-$(read_env_value REQUIRE_MIGRATION_APPROVAL)}"
REQUIRE_MIGRATION_APPROVAL="${REQUIRE_MIGRATION_APPROVAL:-false}"
PUBLIC_URL="${PUBLIC_URL:-$(read_env_value PUBLIC_URL)}"
PUBLIC_URL="${PUBLIC_URL:-https://meghaconnect.cloud}"

if [[ "$REQUIRE_MIGRATION_APPROVAL" == "true" && "$MIGRATIONS_APPROVED" != "true" ]]; then
  die "Migration approval is required. Review Flyway changes/backup policy, then pass --approve-migrations."
fi

exec 9>"$LOCK_FILE"
flock -n 9 || die "Another deployment is already running"
trap on_error ERR

PREVIOUS_TAG="$(cat "$STATE_FILE" 2>/dev/null || read_env_value IMAGE_TAG || true)"
if [[ "$PREVIOUS_TAG" == "sha-replace-with-immutable-commit" ]]; then
  PREVIOUS_TAG=""
elif [[ -n "$PREVIOUS_TAG" && ! "$PREVIOUS_TAG" =~ ^sha-[0-9a-f]{40}$ ]]; then
  die "Recorded previous image tag is not a valid immutable SHA tag"
fi
[[ "$PREVIOUS_TAG" == "$IMAGE_TAG" ]] && die "Tag $IMAGE_TAG is already recorded as deployed"

log "Starting deployment actor=$DEPLOY_ACTOR commit=$DEPLOY_COMMIT previous=$PREVIOUS_TAG new=$IMAGE_TAG"
DEPLOY_STARTED="true"
deploy_tag "$IMAGE_TAG"
HEALTH_OUTCOME="success"
printf '%s\n' "$IMAGE_TAG" > "$STATE_FILE"
chmod 640 "$STATE_FILE"
record_history success
trap - ERR
log "Deployment completed successfully; previous image remains available for rollback"
