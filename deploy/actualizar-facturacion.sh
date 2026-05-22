#!/bin/bash
# Actualiza facturación (abonos): backend + frontend en producción.
# Uso en el VPS:
#   cd /ruta/al/proyecto && git pull && ./deploy/actualizar-facturacion.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/deploy/.env}"
COMPOSE="docker compose -f $ROOT/docker-compose.prod.yml --env-file $ENV_FILE"

cd "$ROOT"
echo "==> git pull..."
git pull

echo "==> Rebuild backend..."
$COMPOSE build --no-cache backend
$COMPOSE up -d backend

echo "==> Rebuild frontend..."
$COMPOSE build --no-cache frontend
$COMPOSE up -d frontend

echo "==> Comprobar que el panel trae la UI de abonos..."
PANEL_HTML="$(curl -sL https://sportgymr10.com/panel/facturacion 2>/dev/null || true)"
if echo "$PANEL_HTML" | grep -q 'Abono parcial'; then
  echo "OK: HTML del panel incluye «Abono parcial»."
elif echo "$PANEL_HTML" | grep -q 'Generar pago y activar'; then
  echo "AVISO: sigue el texto viejo «Generar pago…». Pruebe Ctrl+F5 o limpie caché del navegador."
else
  echo "AVISO: no se pudo verificar el HTML (SPA). Tras desplegar, recargue con Ctrl+F5."
fi

echo "Listo: backend y frontend actualizados."
