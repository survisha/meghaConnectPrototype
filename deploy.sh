#!/usr/bin/env bash
# MeghaConnect UAT deployment script for Ubuntu + Nginx + systemd.
# It can either build from source or install prebuilt artifacts, then
# configures Nginx/systemd idempotently for https://meghaconnect.cloud.
#
# Source mode, default:
#   ./deploy.sh
#
# Prebuilt mode:
#   release/
#     frontend/          # Angular dist/browser contents, must contain index.html
#     backend/app.jar    # Spring Boot executable JAR
#   DEPLOY_MODE=prebuilt ./deploy.sh

set -Eeuo pipefail

DEPLOY_MODE="${DEPLOY_MODE:-source}" # source | prebuilt
DOMAIN="${DOMAIN:-meghaconnect.cloud}"
WWW_DOMAIN="${WWW_DOMAIN:-www.meghaconnect.cloud}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/meghaconnect}"
WEB_ROOT="${WEB_ROOT:-/var/www/meghaconnect}"
FRONTEND_BACKUP_ROOT="${FRONTEND_BACKUP_ROOT:-/var/www/meghaconnect-backups}"
SERVICE_NAME="${SERVICE_NAME:-meghaconnect-api}"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
ENV_DIR="${ENV_DIR:-/etc/meghaconnect}"
ENV_FILE="${ENV_FILE:-${ENV_DIR}/${SERVICE_NAME}.env}"
NGINX_SITE_NAME="${NGINX_SITE_NAME:-meghaconnect}"
NGINX_AVAILABLE="/etc/nginx/sites-available/${NGINX_SITE_NAME}"
NGINX_ENABLED="/etc/nginx/sites-enabled/${NGINX_SITE_NAME}"
SPRING_PROFILE="${SPRING_PROFILE:-dev}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_API_URL="${FRONTEND_API_URL:-https://${DOMAIN}/api/v1}"
PULL_LATEST="${PULL_LATEST:-false}"
ENABLE_CERTBOT="${ENABLE_CERTBOT:-true}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
CONFIGURE_UFW="${CONFIGURE_UFW:-false}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -n "${ARTIFACT_ROOT:-}" ]]; then
  ARTIFACT_ROOT="$(cd "${ARTIFACT_ROOT}" && pwd)"
elif [[ -d "${SCRIPT_DIR}/frontend" && -f "${SCRIPT_DIR}/backend/app.jar" ]]; then
  # Prebuilt bundle layout:
  #   /opt/release/frontend/index.html
  #   /opt/release/backend/app.jar
  #   /opt/release/deploy.sh
  ARTIFACT_ROOT="${SCRIPT_DIR}"
else
  # Repo/source layout or parent bundle layout:
  #   /path/to/repo/release/frontend/index.html
  #   /path/to/repo/release/backend/app.jar
  ARTIFACT_ROOT="${SCRIPT_DIR}/release"
fi

PREBUILT_FRONTEND_DIR="${PREBUILT_FRONTEND_DIR:-${ARTIFACT_ROOT}/frontend}"
PREBUILT_BACKEND_JAR="${PREBUILT_BACKEND_JAR:-${ARTIFACT_ROOT}/backend/app.jar}"

if [[ "${DEPLOY_MODE}" != "source" && "${DEPLOY_MODE}" != "prebuilt" ]]; then
  echo "[ERROR] DEPLOY_MODE must be either 'source' or 'prebuilt'."
  exit 1
fi

if [[ "${DEPLOY_MODE}" == "source" ]]; then
  if [[ -n "${PROJECT_ROOT:-}" ]]; then
    PROJECT_ROOT="$(cd "$PROJECT_ROOT" && pwd)"
  elif [[ -d "${SCRIPT_DIR}/frontend" && -d "${SCRIPT_DIR}/backend" ]]; then
    PROJECT_ROOT="$SCRIPT_DIR"
  elif [[ -d "$(pwd)/frontend" && -d "$(pwd)/backend" ]]; then
    PROJECT_ROOT="$(pwd)"
  else
    echo "[ERROR] Could not locate project root. Run from repo root or set PROJECT_ROOT=/path/to/meghaConnectPrototype."
    exit 1
  fi
else
  PROJECT_ROOT="${PROJECT_ROOT:-${SCRIPT_DIR}}"
fi

FRONTEND_DIR="${PROJECT_ROOT}/frontend"
BACKEND_DIR="${PROJECT_ROOT}/backend"
FRONTEND_DIST_ROOT="${FRONTEND_DIR}/dist/frontend"
APP_USER="${APP_USER:-meghaconnect}"
APP_GROUP="${APP_GROUP:-meghaconnect}"

if [[ "${EUID}" -eq 0 ]]; then
  SUDO=""
else
  SUDO="sudo"
fi

log() {
  echo "[INFO] $*"
}

warn() {
  echo "[WARN] $*"
}

die() {
  echo "[ERROR] $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

run_as_root() {
  # shellcheck disable=SC2086
  $SUDO "$@"
}

create_directories() {
  log "Creating deployment directories"
  run_as_root mkdir -p \
    "${DEPLOY_ROOT}/backend" \
    "${DEPLOY_ROOT}/logs" \
    "${DEPLOY_ROOT}/uploads/dev" \
    "${DEPLOY_ROOT}/uploads/uat" \
    "${DEPLOY_ROOT}/scripts" \
    "${WEB_ROOT}" \
    "${FRONTEND_BACKUP_ROOT}" \
    "${ENV_DIR}"
}

ensure_system_user() {
  if id -u "${APP_USER}" >/dev/null 2>&1; then
    log "System user ${APP_USER} already exists"
  else
    log "Creating system user ${APP_USER}"
    run_as_root useradd --system --home "${DEPLOY_ROOT}" --shell /usr/sbin/nologin "${APP_USER}"
  fi
}

set_permissions() {
  log "Setting directory permissions"
  run_as_root chown -R "${APP_USER}:${APP_GROUP}" "${DEPLOY_ROOT}"
  run_as_root chown -R "www-data:www-data" "${WEB_ROOT}" "${FRONTEND_BACKUP_ROOT}"
  run_as_root chmod 750 "${DEPLOY_ROOT}" "${DEPLOY_ROOT}/backend" "${DEPLOY_ROOT}/scripts"
  run_as_root chmod 770 "${DEPLOY_ROOT}/logs" "${DEPLOY_ROOT}/uploads" "${DEPLOY_ROOT}/uploads/dev" "${DEPLOY_ROOT}/uploads/uat"
  run_as_root chmod 755 "${WEB_ROOT}"
}

pull_latest_if_enabled() {
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    log "Skipping git pull in prebuilt mode"
    return
  fi

  if [[ "${PULL_LATEST}" != "true" ]]; then
    log "Skipping git pull. Set PULL_LATEST=true to pull before build."
    return
  fi

  if [[ -d "${PROJECT_ROOT}/.git" ]]; then
    log "Pulling latest code with git pull --ff-only"
    git -C "${PROJECT_ROOT}" pull --ff-only
  else
    warn "PULL_LATEST=true but ${PROJECT_ROOT} is not a git repository; using existing files."
  fi
}

write_frontend_environment() {
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    log "Skipping Angular environment update in prebuilt mode"
    warn "Make sure the Angular artifact was built with API URL: ${FRONTEND_API_URL}"
    return
  fi

  log "Updating Angular UAT environment: ${FRONTEND_API_URL}"
  cat > "${FRONTEND_DIR}/src/environments/environment.dev.ts" <<EOF_ENV
// UAT/dev-server environment configuration.
// Used by the Angular "uat" build configuration in angular.json.
export const environment = {
  production: true,
  apiUrl: '${FRONTEND_API_URL}',
  appName: 'MeghaConnect [UAT]',
  version: '1.0.0-uat'
};
EOF_ENV
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

build_frontend() {
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    log "Using prebuilt Angular artifact from ${PREBUILT_FRONTEND_DIR}"
    resolve_frontend_build_dir "${PREBUILT_FRONTEND_DIR}"
    return
  fi

  require_command npm
  log "Building Angular UI using configuration uat"
  pushd "${FRONTEND_DIR}" >/dev/null
  if [[ -f package-lock.json ]]; then
    npm ci
  else
    npm install
  fi
  npx ng build --configuration uat
  popd >/dev/null

  resolve_frontend_build_dir "${FRONTEND_DIST_ROOT}"
}

backup_and_install_frontend() {
  local timestamp backup_file
  timestamp="$(date +%Y%m%d%H%M%S)"
  backup_file="${FRONTEND_BACKUP_ROOT}/frontend-${timestamp}.tar.gz"

  if find "${WEB_ROOT}" -mindepth 1 -maxdepth 1 | grep -q .; then
    log "Backing up current frontend to ${backup_file}"
    run_as_root tar -czf "${backup_file}" -C "${WEB_ROOT}" .
    log "Rollback note: restore with 'sudo tar -xzf ${backup_file} -C ${WEB_ROOT}'"
  else
    log "No existing frontend files to back up"
  fi

  log "Installing Angular files to ${WEB_ROOT}"
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
    log "Using prebuilt backend JAR from ${PREBUILT_BACKEND_JAR}"
    [[ -f "${PREBUILT_BACKEND_JAR}" ]] || die "Prebuilt backend JAR not found: ${PREBUILT_BACKEND_JAR}"
    BACKEND_JAR="${PREBUILT_BACKEND_JAR}"
    return
  fi

  require_command mvn
  log "Building Spring Boot backend JAR"
  pushd "${BACKEND_DIR}" >/dev/null
  mvn clean package -DskipTests
  popd >/dev/null

  BACKEND_JAR="$(find "${BACKEND_DIR}/target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" ! -name "*sources*" ! -name "*javadoc*" | head -n 1)"
  [[ -n "${BACKEND_JAR}" && -f "${BACKEND_JAR}" ]] || die "Backend JAR not found under ${BACKEND_DIR}/target"
}

install_backend() {
  log "Installing backend JAR to ${DEPLOY_ROOT}/backend/app.jar"
  run_as_root install -m 640 -o "${APP_USER}" -g "${APP_GROUP}" "${BACKEND_JAR}" "${DEPLOY_ROOT}/backend/app.jar"
}

write_env_file_if_missing() {
  if [[ -f "${ENV_FILE}" ]]; then
    log "Environment file already exists: ${ENV_FILE}"
  else
    log "Creating environment file: ${ENV_FILE}"
    run_as_root tee "${ENV_FILE}" >/dev/null <<EOF_ENV_FILE
SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=${BACKEND_PORT}
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=meghaconnect_db
DB_USERNAME=meghaconnect_user
DB_PASSWORD=CHANGE_ME
FILE_UPLOAD_ROOT=${DEPLOY_ROOT}/uploads/dev
STORAGE_BASE_URL=https://${DOMAIN}/api/uploads
LOGGING_PATH=${DEPLOY_ROOT}/logs
CORS_ALLOWED_ORIGINS=https://${DOMAIN},https://${WWW_DOMAIN}
JWT_SECRET=CHANGE_ME_MIN_256_BIT_SECRET_FOR_UAT_ONLY
JWT_EXPIRATION_MS=86400000
PUBLIC_DARBAR_SCHEDULER_DELAY_MS=60000
EOF_ENV_FILE
    run_as_root chmod 600 "${ENV_FILE}"
    run_as_root chown root:"${APP_GROUP}" "${ENV_FILE}" || true
  fi

  if run_as_root grep -q "DB_PASSWORD=CHANGE_ME" "${ENV_FILE}" 2>/dev/null; then
    warn "Update DB_PASSWORD in ${ENV_FILE} before first successful startup."
  fi
  if run_as_root grep -q "JWT_SECRET=CHANGE_ME" "${ENV_FILE}" 2>/dev/null; then
    warn "Update JWT_SECRET in ${ENV_FILE} before first successful startup."
  fi
}

install_systemd_service() {
  log "Installing systemd service ${SERVICE_NAME}"
  run_as_root tee "${SERVICE_FILE}" >/dev/null <<EOF_SERVICE
[Unit]
Description=MeghaConnect Spring Boot API
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=${APP_USER}
Group=${APP_GROUP}
WorkingDirectory=${DEPLOY_ROOT}/backend
EnvironmentFile=-${ENV_FILE}
ExecStart=/usr/bin/java -jar ${DEPLOY_ROOT}/backend/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
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
  log "Restarting backend service"
  run_as_root systemctl restart "${SERVICE_NAME}"
}

install_nginx_config() {
  require_command nginx
  log "Installing Nginx site ${NGINX_SITE_NAME}"
  if [[ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" && -f "/etc/letsencrypt/live/${DOMAIN}/privkey.pem" ]]; then
    log "Existing SSL certificate found; installing HTTP redirect and HTTPS Nginx site"
    run_as_root tee "${NGINX_AVAILABLE}" >/dev/null <<EOF_NGINX
server {
    listen 80;
    server_name ${DOMAIN} ${WWW_DOMAIN};

    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ${DOMAIN} ${WWW_DOMAIN};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    root ${WEB_ROOT};
    index index.html;

    client_max_body_size 25M;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Request-Id \$http_x_request_id;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 120s;
    }

    location = /health {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/api/actuator/health;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF_NGINX
  else
    log "No SSL certificate found yet; installing HTTP Nginx site"
    run_as_root tee "${NGINX_AVAILABLE}" >/dev/null <<EOF_NGINX
server {
    listen 80;
    server_name ${DOMAIN} ${WWW_DOMAIN};

    root ${WEB_ROOT};
    index index.html;

    client_max_body_size 25M;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Request-Id \$http_x_request_id;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 120s;
    }

    location = /health {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/api/actuator/health;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF_NGINX
  fi

  run_as_root ln -sfn "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
  if [[ -e /etc/nginx/sites-enabled/default ]]; then
    log "Disabling default Nginx site"
    run_as_root rm -f /etc/nginx/sites-enabled/default
  fi
  run_as_root nginx -t
  run_as_root systemctl reload nginx
}

install_certbot_if_possible() {
  if [[ "${ENABLE_CERTBOT}" != "true" ]]; then
    log "Skipping Certbot. Set ENABLE_CERTBOT=true to enable SSL automation."
    return
  fi

  if [[ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]]; then
    log "SSL certificate already exists for ${DOMAIN}"
    return
  fi

  if ! command -v certbot >/dev/null 2>&1; then
    warn "Certbot is not installed. Install it with: sudo apt install certbot python3-certbot-nginx"
    return
  fi

  if [[ -z "${CERTBOT_EMAIL}" ]]; then
    warn "CERTBOT_EMAIL is not set. Skipping non-interactive SSL issuance."
    return
  fi

  log "Requesting SSL certificate with Certbot"
  run_as_root certbot --nginx \
    -d "${DOMAIN}" \
    -d "${WWW_DOMAIN}" \
    --non-interactive \
    --agree-tos \
    -m "${CERTBOT_EMAIL}" \
    --redirect
}

configure_firewall_if_enabled() {
  if [[ "${CONFIGURE_UFW}" != "true" ]]; then
    log "Skipping UFW changes. Set CONFIGURE_UFW=true after confirming SSH access."
    return
  fi

  if ! command -v ufw >/dev/null 2>&1; then
    warn "ufw is not installed; skipping firewall configuration."
    return
  fi

  log "Configuring UFW: allow 22/80/443, deny 8080/3306"
  run_as_root ufw allow OpenSSH
  run_as_root ufw allow 80/tcp
  run_as_root ufw allow 443/tcp
  run_as_root ufw deny 8080/tcp
  run_as_root ufw deny 3306/tcp
  run_as_root ufw --force enable
  run_as_root ufw status verbose
}

install_script_copy() {
  local target_script current_script
  target_script="${DEPLOY_ROOT}/scripts/deploy.sh"
  current_script="$(readlink -f "${BASH_SOURCE[0]}")"
  if [[ "${current_script}" != "${target_script}" ]]; then
    log "Copying deploy script to ${target_script}"
    run_as_root install -m 750 -o "${APP_USER}" -g "${APP_GROUP}" "${current_script}" "${target_script}"
  fi
}

validate_deployment() {
  log "Validating backend service status"
  run_as_root systemctl --no-pager --full status "${SERVICE_NAME}" || true

  log "Waiting for backend health endpoint"
  for attempt in {1..30}; do
    if curl -fsS "http://127.0.0.1:${BACKEND_PORT}/api/actuator/health" >/tmp/meghaconnect-health.json 2>/dev/null; then
      cat /tmp/meghaconnect-health.json
      echo
      break
    fi
    if [[ "${attempt}" -eq 30 ]]; then
      warn "Health check did not pass after 30 attempts. Check: sudo journalctl -u ${SERVICE_NAME} -f"
    else
      sleep 2
    fi
  done

  log "Validating Nginx"
  run_as_root nginx -t

  if curl -fsSI "http://127.0.0.1" >/dev/null 2>&1; then
    log "Local Nginx HTTP check passed"
  else
    warn "Local Nginx HTTP check failed"
  fi

  if [[ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]]; then
    if curl -fsSI "https://${DOMAIN}" >/dev/null 2>&1; then
      log "Public HTTPS check passed: https://${DOMAIN}"
    else
      warn "Public HTTPS check failed. Verify DNS points ${DOMAIN} to this VPS."
    fi
  else
    warn "SSL certificate is not installed yet; HTTPS check skipped."
  fi
}

main() {
  log "Starting MeghaConnect UAT deployment"
  log "Deploy mode: ${DEPLOY_MODE}"
  log "Project root: ${PROJECT_ROOT}"
  if [[ "${DEPLOY_MODE}" == "prebuilt" ]]; then
    log "Artifact root: ${ARTIFACT_ROOT}"
  fi
  log "Domain: ${DOMAIN}"

  require_command java
  require_command curl
  require_command tar

  create_directories
  ensure_system_user
  set_permissions
  pull_latest_if_enabled
  write_frontend_environment
  build_frontend
  backup_and_install_frontend
  build_backend
  install_backend
  write_env_file_if_missing
  install_systemd_service
  restart_backend
  install_nginx_config
  install_certbot_if_possible
  configure_firewall_if_enabled
  install_script_copy
  validate_deployment

  log "Deployment completed successfully"
  log "UI: https://${DOMAIN}"
  log "API health: https://${DOMAIN}/api/actuator/health"
}

main "$@"
