#!/bin/bash
# Diagnóstico rápido cuando sportgymr10.com devuelve 502 en /api/*
# Uso en el VPS: cd /apps/gym-app && ./deploy/diagnostico-backend.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/deploy/.env}"
COMPOSE="docker compose -f $ROOT/docker-compose.prod.yml --env-file $ENV_FILE"

cd "$ROOT"
echo "=== Contenedores ==="
$COMPOSE ps

echo ""
echo "=== Últimas líneas del backend ==="
docker logs gym-backend --tail 80 2>&1 || true

echo ""
echo "=== Health local (8081) ==="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:8081/api/health || echo "FALLO: backend no responde en 127.0.0.1:8081"

echo ""
echo "=== Health público ==="
curl -sS -o /dev/null -w "HTTP %{http_code}\n" https://sportgymr10.com/api/health || echo "FALLO: 502 o sin conexión"

echo ""
echo "Si el backend está Exited o Restarting, ejecute:"
echo "  git pull && docker compose -f docker-compose.prod.yml --env-file deploy/.env build --no-cache backend"
echo "  docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d backend"
