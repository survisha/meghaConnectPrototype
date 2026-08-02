#!/usr/bin/env bash
# MeghaConnect Pilot/Production deployment script.
# This is intentionally separate from deploy.sh so UAT remains untouched.

set -Eeuo pipefail

DEPLOY_MODE="${DEPLOY_MODE:-source}" # source | prebuilt
DOMAIN="${DOMAIN:-www.meghaconnect.com}"
APEX_DOMAIN="${APEX_DOMAIN:-meghaconnect.com}"
PROD_PORT="${PROD_PORT:-8082}"
UAT_PORT="${UAT_PORT:-8080}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
SERVICE_NAME="${SERVICE_NAME:-meghaconnect-api-prod}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/meghaconnect/prod}"
BACKEND_DEPLOY_DIR="${BACKEND_DEPLOY_DIR:-${DEPLOY_ROOT}/backend}"
BACKEND_BACKUP_DIR="${BACKEND_BACKUP_DIR:-${DEPLOY_ROOT}/backups/backend}"
WEB_ROOT="${WEB_ROOT:-/var/www/meghaconnect-prod}"
FRONTEND_BACKUP_ROOT="${FRONTEND_BACKUP_ROOT:-/var/www/meghaconnect-prod-backups}"
LOG_DIR="${LOG_DIR:-/var/log/meghaconnect/prod}"
ENV_DIR="${ENV_DIR:-/etc/meghaconnect}"
ENV_FILE="${ENV_FILE:-${ENV_DIR}/${SERVICE_NAME}.env}"
NGINX_SITE_NAME="${NGINX_SITE_NAME:-meghaconnect-prod}"
NGINX_AVAILABLE="/etc/nginx/sites-available/${NGINX_SITE_NAME}"
NGINX_ENABLED="/etc/nginx/sites-enabled/${NGINX_SITE_NAME}"
APP_USER="${APP_USER:-meghaconnect}"
APP_GROUP="${APP_GROUP:-meghaconnect}"
FRONTEND_API_URL="${FRONTEND_API_URL:-https://${DOMAIN}/api/v1}"
DB_NAME="${DB_NAME:-meghaconnect_prod}"
PULL_LATEST="${PULL_LATEST:-false}"
ENABLE_CERTBOT="${ENABLE_CERTBOT:-false}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -n "${ARTIFACT_ROOT:-}" ]]; then
  ARTIFACT_ROOT="$(cd "${ARTIFACT_ROOT}" && pwd)"
elif [[ -d "${SCRIPT_DIR}/frontend" && -f "${SCRIPT_DIR}/backend/app.jar" ]]; then
  # Prebuilt bundle layout:
  #   /opt/release-prod/frontend/index.html
  #   /opt/release-prod/backend/app.jar
  #   /opt/release-prod/deploy-prod.sh
  ARTIFACT_ROOT="${SCRIPT_DIR}"
elif [[ -d "/opt/release-prod/frontend" && -f "/opt/release-prod/backend/app.jar" ]]; then
  ARTIFACT_ROOT="/opt/release-prod"
else
  ARTIFACT_ROOT="${SCRIPT_DIR}/release-prod"
fi

if [[ "${EUID}" -eq 0 ]]; then
  SUDO=""
else
  SUDO="sudo"
fi

log() { echo "[INFO] $*"; }
warn() { echo "[WARN] $*"; }
die() { echo "[ERROR] $*" >&2; exit 1; }
run_as_root() { $SUDO "$@"; }
require_command() { command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"; }

set_env_file_value() {
  local key="$1"
  local value="$2"
  if run_as_root grep -q "^${key}=" "${ENV_FILE}" 2>/dev/null; then
    run_as_root sed -i "s|^${key}=.*|${key}=${value}|" "${ENV_FILE}"
  else
    echo "${key}=${value}" | run_as_root tee -a "${ENV_FILE}" >/dev/null
  fi
}

if [[ "${DEPLOY_MODE}" != "source" && "${DEPLOY_MODE}" != "prebuilt" ]]; then
  die "DEPLOY_MODE must be source or prebuilt."
fi

if [[ "${PROD_PORT}" == "${UAT_PORT}" ]]; then
  die "PROD_PORT (${PROD_PORT}) must not equal UAT_PORT (${UAT_PORT})."
fi

if [[ "${DB_NAME}" != *_prod ]]; then
  die "Production DB_NAME must end with _prod. Current DB_NAME=${DB_NAME}"
fi

if [[ "${DEPLOY_MODE}" == "source" ]]; then
  if [[ -n "${PROJECT_ROOT:-}" ]]; then
    PROJECT_ROOT="$(cd "${PROJECT_ROOT}" && pwd)"
  elif [[ -d "${SCRIPT_DIR}/frontend" && -d "${SCRIPT_DIR}/backend" ]]; then
    PROJECT_ROOT="${SCRIPT_DIR}"
  else
    die "Could not locate project root. Run from repo root or set PROJECT_ROOT=/path/to/meghaConnectPrototype."
  fi
else
  PROJECT_ROOT="${PROJECT_ROOT:-${SCRIPT_DIR}}"
fi

FRONTEND_DIR="${PROJECT_ROOT}/frontend"
BACKEND_DIR="${PROJECT_ROOT}/backend"
FRONTEND_DIST_ROOT="${FRONTEND_DIR}/dist/frontend"
PREBUILT_FRONTEND_DIR="${PREBUILT_FRONTEND_DIR:-${ARTIFACT_ROOT}/frontend}"
PREBUILT_BACKEND_JAR="${PREBUILT_BACKEND_JAR:-${ARTIFACT_ROOT}/backend/app.jar}"

create_directories() {
  log "Creating production deployment directories"
  run_as_root mkdir -p \
    "${BACKEND_DEPLOY_DIR}" \
    "${BACKEND_BACKUP_DIR}" \
    "${DEPLOY_ROOT}/uploads" \
    "${DEPLOY_ROOT}/secure" \
    "${DEPLOY_ROOT}/scripts" \
    "${WEB_ROOT}" \
    "${FRONTEND_BACKUP_ROOT}" \
    "${LOG_DIR}" \
    "${ENV_DIR}"
}

ensure_system_user() {
  if getent group "${APP_GROUP}" >/dev/null 2>&1; then
    log "System group ${APP_GROUP} already exists"
  else
    log "Creating system group ${APP_GROUP}"
    run_as_root groupadd --system "${APP_GROUP}"
  fi

  if id -u "${APP_USER}" >/dev/null 2>&1; then
    log "System user ${APP_USER} already exists"
  else
    log "Creating system user ${APP_USER}"
    run_as_root useradd --system --gid "${APP_GROUP}" --home "${DEPLOY_ROOT}" --shell /usr/sbin/nologin "${APP_USER}"
  fi
}

set_permissions() {
  log "Setting production permissions"
  run_as_root chown -R "${APP_USER}:${APP_GROUP}" "${DEPLOY_ROOT}" "${LOG_DIR}"
  run_as_root chown -R "www-data:www-data" "${WEB_ROOT}" "${FRONTEND_BACKUP_ROOT}"
  run_as_root chmod 750 "${DEPLOY_ROOT}" "${BACKEND_DEPLOY_DIR}" "${DEPLOY_ROOT}/scripts"
  run_as_root chmod 770 "${DEPLOY_ROOT}/uploads" "${DEPLOY_ROOT}/secure" "${LOG_DIR}"
  run_as_root chmod 755 "${WEB_ROOT}"
}

pull_latest_if_enabled() {
  if [[ "${DEPLOY_MODE}" == "prebuilt" || "${PULL_LATEST}" != "true" ]]; then
    return
  fi
  [[ -d "${PROJECT_ROOT}/.git" ]] || die "PULL_LATEST=true but ${PROJECT_ROOT} is not a git repository."
  git -C "${PROJECT_ROOT}" pull --ff-only
}

validate_frontend_environment() {
  local env_file="${FRONTEND_DIR}/src/environments/environment.prod.ts"
  [[ -f "${env_file}" ]] || die "Missing ${env_file}"
  grep -q "production: '/api/v1'" "${FRONTEND_DIR}/src/environments/environment.urls.ts" \
    || grep -q "https://www.meghaconnect.com/api/v1" "${FRONTEND_DIR}/src/environments/environment.urls.ts" \
    || grep -q "https://www.meghaconnect.com/api/v1" "${env_file}" \
    || die "Production API URL must be same-origin /api/v1 or https://www.meghaconnect.com/api/v1"
}

validate_backend_config() {
  local config_file="${BACKEND_DIR}/src/main/resources/application-prod.yml"
  [[ -f "${config_file}" ]] || die "Missing ${config_file}"
  grep -q "_prod" "${config_file}" || die "application-prod.yml must reference a _prod database default."
  grep -q "SERVER_PORT:8082" "${config_file}" || warn "application-prod.yml does not default SERVER_PORT to 8082; ENV_FILE will enforce it."
}

resolve_frontend_build_dir() {
  local candidate="$1"
  if [[ -f "${candidate}/index.html" ]]; then
    FRONTEND_BUILD_DIR="${candidate}"
  elif [[ -f "${candidate}/browser/index.html" ]]; then
    FRONTEND_BUILD_DIR="${candidate}/browser"
  else
    die "Angular artifact must contain index.html at ${candidate} or ${candidate}/browser"
  fi
}

resolve_prebuilt_frontend_dir() {
  if [[ -d "${PREBUILT_FRONTEND_DIR}" ]]; then
    return
  fi

  local fallback
  for fallback in \
    "${ARTIFACT_ROOT}/frontend" \
    "${SCRIPT_DIR}/frontend" \
    "/opt/release-prod/frontend"; do
    if [[ -d "${fallback}" ]]; then
      warn "Prebuilt frontend path not found: ${PREBUILT_FRONTEND_DIR}. Using ${fallback}"
      PREBUILT_FRONTEND_DIR="${fallback}"
      return
    fi
  done
}

resolve_prebuilt_backend_jar() {
  if [[ -f "${PREBUILT_BACKEND_JAR}" ]]; then
    return
  fi

  local fallback
  for fallback in \
    "${ARTIFACT_ROOT}/backend/app.jar" \
    "${SCRIPT_DIR}/backend/app.jar" \
    "/opt/release-prod/backend/app.jar"; do
    if [[ -f "${fallback}" ]]; then
      warn "Prebuilt backend JAR not found: ${PREBUILT_BACKEND_JAR}. Using ${fallback}"
      PREBUILT_BACKEND_JAR="${fallback}"
      return
    fi
  done
}

build_frontend() {
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    resolve_prebuilt_frontend_dir
    log "Using prebuilt Angular production artifact from ${PREBUILT_FRONTEND_DIR}"
    resolve_frontend_build_dir "${PREBUILT_FRONTEND_DIR}"
    return
  fi

  require_command npm
  validate_frontend_environment
  log "Building Angular production UI"
  pushd "${FRONTEND_DIR}" >/dev/null
  if [[ -f package-lock.json ]]; then
    npm ci
  else
    npm install
  fi
  npx ng build --configuration production
  popd >/dev/null
  resolve_frontend_build_dir "${FRONTEND_DIST_ROOT}"
}

backup_and_install_frontend() {
  local timestamp backup_file
  timestamp="$(date +%Y%m%d%H%M%S)"
  backup_file="${FRONTEND_BACKUP_ROOT}/frontend-prod-${timestamp}.tar.gz"

  if find "${WEB_ROOT}" -mindepth 1 -maxdepth 1 | grep -q .; then
    log "Backing up current production frontend to ${backup_file}"
    run_as_root tar -czf "${backup_file}" -C "${WEB_ROOT}" .
  fi

  log "Installing production Angular files to ${WEB_ROOT}"
  run_as_root find "${WEB_ROOT}" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  if command -v rsync >/dev/null 2>&1; then
    run_as_root rsync -a "${FRONTEND_BUILD_DIR}/" "${WEB_ROOT}/"
  else
    run_as_root cp -a "${FRONTEND_BUILD_DIR}/." "${WEB_ROOT}/"
  fi
  run_as_root chown -R "www-data:www-data" "${WEB_ROOT}"
  run_as_root find "${WEB_ROOT}" -type d -exec chmod 755 {} +
  run_as_root find "${WEB_ROOT}" -type f -exec chmod 644 {} +
}

build_backend() {
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    resolve_prebuilt_backend_jar
    log "Using prebuilt backend JAR from ${PREBUILT_BACKEND_JAR}"
    [[ -f "${PREBUILT_BACKEND_JAR}" ]] || die "Prebuilt backend JAR not found: ${PREBUILT_BACKEND_JAR}"
    BACKEND_JAR="${PREBUILT_BACKEND_JAR}"
    return
  fi

  require_command mvn
  validate_backend_config
  log "Building Spring Boot backend JAR"
  pushd "${BACKEND_DIR}" >/dev/null
  mvn clean package -DskipTests
  popd >/dev/null
  BACKEND_JAR="$(find "${BACKEND_DIR}/target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" ! -name "*sources*" ! -name "*javadoc*" | head -n 1)"
  [[ -n "${BACKEND_JAR}" && -f "${BACKEND_JAR}" ]] || die "Backend JAR not found under ${BACKEND_DIR}/target"
}

backup_and_install_backend() {
  local timestamp
  timestamp="$(date +%Y%m%d%H%M%S)"
  if [[ -f "${BACKEND_DEPLOY_DIR}/app.jar" ]]; then
    log "Backing up current production backend JAR"
    run_as_root cp "${BACKEND_DEPLOY_DIR}/app.jar" "${BACKEND_BACKUP_DIR}/app-${timestamp}.jar"
  fi
  log "Installing production backend JAR to ${BACKEND_DEPLOY_DIR}/app.jar"
  run_as_root install -m 640 -o "${APP_USER}" -g "${APP_GROUP}" "${BACKEND_JAR}" "${BACKEND_DEPLOY_DIR}/app.jar"
}

write_env_file_if_missing() {
  if [[ -f "${ENV_FILE}" ]]; then
    log "Environment file exists: ${ENV_FILE}"
  else
    log "Creating production environment file: ${ENV_FILE}"
    run_as_root tee "${ENV_FILE}" >/dev/null <<EOF_ENV
SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=${PROD_PORT}
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=${DB_NAME}
DB_USERNAME=
DB_PASSWORD=
DATABASE_USERNAME=
DATABASE_PASSWORD=
FILE_UPLOAD_ROOT=${DEPLOY_ROOT}/uploads
FILE_UPLOAD_CRYPTO_KEY_PATH=${DEPLOY_ROOT}/secure/crypto.key
STORAGE_BASE_URL=https://${DOMAIN}/api/uploads
LOGGING_PATH=${LOG_DIR}
CORS_ALLOWED_ORIGINS=https://${DOMAIN},https://${APEX_DOMAIN}
JWT_SECRET=
JWT_EXPIRATION_MS=86400000
PUBLIC_DARBAR_SCHEDULER_DELAY_MS=60000
EOF_ENV
    run_as_root chmod 600 "${ENV_FILE}"
    run_as_root chown root:"${APP_GROUP}" "${ENV_FILE}" || true
  fi

  set_env_file_value "CORS_ALLOWED_ORIGINS" "https://${DOMAIN},https://${APEX_DOMAIN},http://${DOMAIN},http://${APEX_DOMAIN}"
  set_env_file_value "CORS_ALLOWED_ORIGIN_PATTERNS" "https://${DOMAIN},https://${APEX_DOMAIN},http://${DOMAIN},http://${APEX_DOMAIN}"

  run_as_root grep -q "^DB_NAME=.*_prod" "${ENV_FILE}" || die "${ENV_FILE} DB_NAME must end with _prod."
  run_as_root grep -q "^SERVER_PORT=${UAT_PORT}$" "${ENV_FILE}" && die "${ENV_FILE} uses UAT port ${UAT_PORT}."
  if run_as_root grep -q "^JWT_SECRET=$" "${ENV_FILE}" 2>/dev/null; then
    warn "Set JWT_SECRET in ${ENV_FILE} before first production startup."
  fi
  if run_as_root grep -q "^DB_PASSWORD=$" "${ENV_FILE}" 2>/dev/null; then
    warn "Set DB credentials in ${ENV_FILE} before first production startup."
  fi
}

install_systemd_service() {
  local service_file="/etc/systemd/system/${SERVICE_NAME}.service"
  log "Installing production systemd service ${SERVICE_NAME}"
  run_as_root tee "${service_file}" >/dev/null <<EOF_SERVICE
[Unit]
Description=MeghaConnect Pilot/Production Spring Boot API
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=${APP_USER}
Group=${APP_GROUP}
WorkingDirectory=${BACKEND_DEPLOY_DIR}
EnvironmentFile=-${ENV_FILE}
ExecStart=/usr/bin/java -jar ${BACKEND_DEPLOY_DIR}/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=append:${LOG_DIR}/console.log
StandardError=append:${LOG_DIR}/error.log
SyslogIdentifier=${SERVICE_NAME}
UMask=0027
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
EOF_SERVICE
  run_as_root systemctl daemon-reload
  run_as_root systemctl enable "${SERVICE_NAME}"
}

restart_backend() {
  log "Restarting production backend service only: ${SERVICE_NAME}"
  run_as_root systemctl restart "${SERVICE_NAME}"
}

install_nginx_config() {
  require_command nginx
  log "Installing production Nginx site ${NGINX_SITE_NAME}"
  run_as_root tee "${NGINX_AVAILABLE}" >/dev/null <<EOF_NGINX
server {
    listen 80;
    server_name ${APEX_DOMAIN} ${DOMAIN};

    root ${WEB_ROOT};
    index index.html;
    client_max_body_size 25M;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/v1/visitor-form-extraction/ {
        proxy_pass http://127.0.0.1:${PROD_PORT}/api/v1/visitor-form-extraction/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header Authorization \$http_authorization;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Request-Id \$request_id;
        proxy_connect_timeout 30s;
        proxy_send_timeout 420s;
        proxy_read_timeout 420s;
        send_timeout 420s;
        proxy_buffering off;
    }

    location /api/v1/ {
        proxy_pass http://127.0.0.1:${PROD_PORT}/api/v1/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header Authorization \$http_authorization;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Request-Id \$http_x_request_id;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 120s;
    }

    location /api/actuator/health {
        proxy_pass http://127.0.0.1:${PROD_PORT}/api/actuator/health;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header Authorization \$http_authorization;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}

# If SSL is already issued, enable this block and keep the HTTP block as redirect.
# server {
#     listen 443 ssl http2;
#     server_name ${APEX_DOMAIN} ${DOMAIN};
#     ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
#     ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
#     include /etc/letsencrypt/options-ssl-nginx.conf;
#     ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
#     root ${WEB_ROOT};
#     index index.html;
#     client_max_body_size 25M;
#     location / { try_files \$uri \$uri/ /index.html; }
#     location /api/v1/ {
#         proxy_pass http://127.0.0.1:${PROD_PORT}/api/v1/;
#         proxy_http_version 1.1;
#         proxy_set_header Host \$host;
#         proxy_set_header Authorization \$http_authorization;
#         proxy_set_header X-Real-IP \$remote_addr;
#         proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
#         proxy_set_header X-Forwarded-Proto \$scheme;
#     }
# }
EOF_NGINX
  run_as_root ln -sfn "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
  run_as_root nginx -t
  run_as_root systemctl reload nginx
}

issue_ssl_if_enabled() {
  if [[ "${ENABLE_CERTBOT}" != "true" ]]; then
    log "Skipping Certbot. Set ENABLE_CERTBOT=true CERTBOT_EMAIL=name@example.com to issue SSL."
    return
  fi
  command -v certbot >/dev/null 2>&1 || die "Certbot not found. Install certbot python3-certbot-nginx first."
  [[ -n "${CERTBOT_EMAIL}" ]] || die "CERTBOT_EMAIL is required when ENABLE_CERTBOT=true."
  run_as_root certbot --nginx -d "${APEX_DOMAIN}" -d "${DOMAIN}" --non-interactive --agree-tos -m "${CERTBOT_EMAIL}" --redirect
}

validate_deployment() {
  require_command curl
  log "Checking production backend health"
  for attempt in {1..30}; do
    if curl -fsS "http://127.0.0.1:${PROD_PORT}/api/actuator/health" >/tmp/meghaconnect-prod-health.json 2>/dev/null; then
      cat /tmp/meghaconnect-prod-health.json
      echo
      return
    fi
    sleep 2
  done
  warn "Production health check failed. Check logs: sudo journalctl -u ${SERVICE_NAME} -f or ${LOG_DIR}"
}

main() {
  log "Starting MeghaConnect Pilot/Production deployment"
  log "Production frontend: https://${DOMAIN}"
  log "Production API: ${FRONTEND_API_URL}"
  log "Backend local: http://127.0.0.1:${PROD_PORT}/api/v1"
  log "UAT deploy.sh is not modified or stopped."
  log "Deploy mode: ${DEPLOY_MODE}"
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    log "Artifact root: ${ARTIFACT_ROOT}"
  fi

  require_command java
  require_command curl
  require_command tar
  create_directories
  ensure_system_user
  set_permissions
  pull_latest_if_enabled
  build_frontend
  backup_and_install_frontend
  build_backend
  backup_and_install_backend
  write_env_file_if_missing
  install_systemd_service
  restart_backend
  install_nginx_config
  issue_ssl_if_enabled
  validate_deployment

  log "Pilot/Production deployment completed"
  log "Frontend: https://${DOMAIN}"
  log "API: https://${DOMAIN}/api/v1"
  log "Backend local: http://127.0.0.1:${PROD_PORT}/api/v1"
}

main "$@"
