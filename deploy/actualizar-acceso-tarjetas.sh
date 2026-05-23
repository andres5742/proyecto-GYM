#!/bin/bash
# Backend + frontend: UID de tarjeta (A5214A48), pantalla acceso y registro.
# Uso en el VPS:
#   cd /apps/gym-app && ./deploy/actualizar-acceso-tarjetas.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/deploy/.env}"
COMPOSE="docker compose -f $ROOT/docker-compose.prod.yml --env-file $ENV_FILE"

cd "$ROOT"
echo "==> git pull..."
git pull

echo "==> Rebuild backend + frontend..."
$COMPOSE build --no-cache backend frontend

echo "==> Reiniciar contenedores..."
$COMPOSE up -d backend frontend

echo "==> Health..."
curl -sf "http://127.0.0.1:8081/api/health" | head -c 200
echo ""

echo "==> Últimas lecturas de tarjeta en logs (si hay):"
docker logs gym-backend --tail 200 2>&1 | grep -iE 'zkt|card|pin|access' | tail -15 || true

echo "OK. Pruebe en recepción: Acceso → pasar tarjeta → debe verse UID tipo A5214A48."
