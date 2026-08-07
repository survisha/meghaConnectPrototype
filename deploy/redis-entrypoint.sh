#!/bin/sh
set -eu

if [ -z "${REDIS_PASSWORD:-}" ]; then
  echo "REDIS_PASSWORD is required" >&2
  exit 1
fi

case "$REDIS_PASSWORD" in
  *[!A-Za-z0-9_.@%+=:-]*)
    echo "REDIS_PASSWORD must use the documented safe random character set" >&2
    exit 1
    ;;
esac

umask 077
cat > /tmp/redis.conf <<EOF
bind 0.0.0.0
protected-mode yes
port 6379
requirepass ${REDIS_PASSWORD}
dir /data
appendonly yes
appendfsync everysec
maxmemory ${REDIS_MAXMEMORY:-384mb}
maxmemory-policy ${REDIS_MAXMEMORY_POLICY:-allkeys-lru}
save 900 1
save 300 10
save 60 10000
EOF

unset REDIS_PASSWORD
exec redis-server /tmp/redis.conf
