#!/usr/bin/env bash
set -Eeuo pipefail

[[ "${EUID}" -eq 0 ]] || { echo "Run once as root" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_USER="${DEPLOY_USER:-meghadeploy}"
APP_GROUP="${APP_GROUP:-meghaconnect}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/meghaconnect/deploy}"
ENV_DIR="${ENV_DIR:-/etc/meghaconnect}"
DOMAIN="${DOMAIN:-meghaconnect.cloud}"
WWW_DOMAIN="${WWW_DOMAIN:-www.meghaconnect.cloud}"
CONFIGURE_UFW="${CONFIGURE_UFW:-false}"

log() { echo "$(date -u +'%Y-%m-%dT%H:%M:%SZ') [bootstrap] $*"; }

install_deployment_file() {
  local mode="$1" source="$2" destination="$3"
  if [[ -e "$destination" && "$source" -ef "$destination" ]]; then
    chown "$DEPLOY_USER:$APP_GROUP" "$destination"
    chmod "$mode" "$destination"
    return
  fi
  install -m "$mode" -o "$DEPLOY_USER" -g "$APP_GROUP" "$source" "$destination"
}

install_docker() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    log "Docker Engine and Compose plugin already available"
    return
  fi
  . /etc/os-release
  [[ "$ID" == "ubuntu" ]] || { echo "This bootstrap supports Ubuntu only" >&2; exit 1; }
  apt-get update
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
}

create_users_and_paths() {
  getent group "$APP_GROUP" >/dev/null || groupadd --system "$APP_GROUP"
  if ! id "$DEPLOY_USER" >/dev/null 2>&1; then
    useradd --create-home --shell /bin/bash "$DEPLOY_USER"
  fi
  usermod -aG docker,"$APP_GROUP" "$DEPLOY_USER"
  log "WARNING: membership in the docker group is effectively root-equivalent"

  install -d -m 0750 -o "$DEPLOY_USER" -g "$APP_GROUP" "$DEPLOY_ROOT" "$DEPLOY_ROOT/history"
  # A shared setgid group keeps the legacy systemd user and the non-root container
  # able to use the same persistent files throughout the staged migration.
  install -d -m 2770 -o root -g "$APP_GROUP" /opt/meghaconnect/uploads /opt/meghaconnect/uploads/uat /opt/meghaconnect/logs
  install -d -m 0750 -o root -g "$APP_GROUP" "$ENV_DIR" "$ENV_DIR/secrets"
}

install_deployment_files() {
  local app_gid
  app_gid="$(getent group "$APP_GROUP" | cut -d: -f3)"
  install_deployment_file 0750 "$SCRIPT_DIR/deploy-docker.sh" "$DEPLOY_ROOT/deploy-docker.sh"
  install_deployment_file 0640 "$SCRIPT_DIR/docker-compose.uat.yml" "$DEPLOY_ROOT/docker-compose.uat.yml"
  install_deployment_file 0640 "$SCRIPT_DIR/prometheus.uat.yml" "$DEPLOY_ROOT/prometheus.uat.yml"
  install_deployment_file 0750 "$SCRIPT_DIR/redis-entrypoint.sh" "$DEPLOY_ROOT/redis-entrypoint.sh"

  install -d -m 0750 -o "$DEPLOY_USER" -g "$APP_GROUP" \
    "$DEPLOY_ROOT/monitoring/alerts" \
    "$DEPLOY_ROOT/monitoring/grafana/provisioning"
  cp -a "$SCRIPT_DIR/../monitoring/alerts/." "$DEPLOY_ROOT/monitoring/alerts/"
  cp -a "$SCRIPT_DIR/../monitoring/grafana/provisioning/." "$DEPLOY_ROOT/monitoring/grafana/provisioning/"
  install -m 0640 -o "$DEPLOY_USER" -g "$APP_GROUP" "$SCRIPT_DIR/../monitoring/loki-config.yml" "$DEPLOY_ROOT/monitoring/loki-config.yml"
  install -m 0640 -o "$DEPLOY_USER" -g "$APP_GROUP" "$SCRIPT_DIR/../monitoring/promtail-config.yml" "$DEPLOY_ROOT/monitoring/promtail-config.yml"
  chown -R "$DEPLOY_USER:$APP_GROUP" "$DEPLOY_ROOT/monitoring"

  if [[ ! -f "$DEPLOY_ROOT/.env" ]]; then
    install -m 0640 -o "$DEPLOY_USER" -g "$APP_GROUP" "$SCRIPT_DIR/.env.uat.example" "$DEPLOY_ROOT/.env"
    sed -i "s/^HOST_APP_GID=.*/HOST_APP_GID=${app_gid}/" "$DEPLOY_ROOT/.env"
    log "Created $DEPLOY_ROOT/.env from a non-secret template; update it before deployment"
  else
    log "Preserving existing $DEPLOY_ROOT/.env"
  fi
  if grep -q '^HOST_APP_GID=replace-with-host-group-id$' "$DEPLOY_ROOT/.env"; then
    sed -i "s/^HOST_APP_GID=.*/HOST_APP_GID=${app_gid}/" "$DEPLOY_ROOT/.env"
  elif ! grep -q '^HOST_APP_GID=' "$DEPLOY_ROOT/.env"; then
    printf '\nHOST_APP_GID=%s\n' "$app_gid" >> "$DEPLOY_ROOT/.env"
  fi

  if [[ ! -f "$ENV_DIR/meghaconnect-api.env" ]]; then
    install -m 0640 -o root -g "$APP_GROUP" "$SCRIPT_DIR/meghaconnect-api.env.example" "$ENV_DIR/meghaconnect-api.env.template"
    log "Created environment template only; populate $ENV_DIR/meghaconnect-api.env manually"
  else
    log "Preserving existing $ENV_DIR/meghaconnect-api.env"
    chown root:"$APP_GROUP" "$ENV_DIR/meghaconnect-api.env"
    chmod 0640 "$ENV_DIR/meghaconnect-api.env"
  fi
}

install_nginx_candidate() {
  command -v nginx >/dev/null 2>&1 || apt-get install -y nginx
  local candidate="/etc/nginx/sites-available/meghaconnect-docker-candidate"
  sed -e "s/__DOMAIN__/${DOMAIN}/g" -e "s/__WWW_DOMAIN__/${WWW_DOMAIN}/g" \
    "$SCRIPT_DIR/nginx-meghaconnect-docker.conf.template" > "$candidate"
  chmod 0644 "$candidate"
  log "Installed inactive Nginx candidate at $candidate; existing enabled site and Certbot files were not changed"
}

install_logrotate() {
  cat > /etc/logrotate.d/meghaconnect-container-files <<'EOF'
/opt/meghaconnect/logs/*.log {
  daily
  rotate 30
  compress
  delaycompress
  missingok
  notifempty
  copytruncate
  su root meghaconnect
}
EOF
}

configure_firewall() {
  [[ "$CONFIGURE_UFW" == "true" ]] || { log "UFW unchanged; set CONFIGURE_UFW=true only after confirming SSH access"; return; }
  command -v ufw >/dev/null 2>&1 || apt-get install -y ufw
  ufw allow OpenSSH
  ufw allow 80/tcp
  ufw allow 443/tcp
  ufw deny 8080/tcp
  ufw deny 8081/tcp
  ufw deny 9090/tcp
  ufw deny 9091/tcp
  ufw deny 3000/tcp
  ufw deny 3100/tcp
  ufw --force enable
}

install_docker
create_users_and_paths
install_deployment_files
install_nginx_candidate
install_logrotate
configure_firewall

log "Bootstrap completed without changing the enabled Nginx site or systemd application service"
log "Configure SSH key-only access for $DEPLOY_USER and perform: docker login ghcr.io"
log "Use alternate host ports for Stage 1 while the systemd service remains active"
