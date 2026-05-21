#!/bin/bash
# Reconstruye SOLO el frontend en producción (pantalla /acceso del torniquete).
# Uso en el VPS:
#   cd /ruta/al/proyecto && git pull && ./deploy/actualizar-pantalla-acceso.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/deploy/.env}"
COMPOSE="docker compose -f $ROOT/docker-compose.prod.yml --env-file $ENV_FILE"

cd "$ROOT"
echo "==> git pull..."
git pull

echo "==> Rebuild frontend (sin caché de Docker)..."
$COMPOSE build --no-cache frontend

echo "==> Reiniciar contenedor frontend..."
$COMPOSE up -d frontend

echo "==> Comprobar que el bundle ya no trae el logo viejo..."
MAIN_JS="$(curl -sL https://sportgymr10.com/acceso | sed -n 's/.*src=\"\\(main-[^\"]*\\.js\\)\".*/\\1/p' | head -1)"
if [[ -z "$MAIN_JS" ]]; then
  echo "AVISO: no se detectó main-*.js en /acceso"
  exit 0
fi
if curl -sL "https://sportgymr10.com/$MAIN_JS" | grep -q 'access-card'; then
  echo "ERROR: el servidor SIGUE con JS viejo (access-card). Revise nginx o URL."
  exit 1
fi
echo "OK: $MAIN_JS sin access-card — pantalla acceso actualizada."
