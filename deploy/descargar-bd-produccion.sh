#!/bin/bash
# Descarga la BD de producción a tu PC (volcado por SSH, sin guardar archivo en el VPS).
#
# Uso:
#   export VPS_HOST=72.61.65.92
#   export VPS_USER=root
#   export VPS_APP_DIR=/apps/gym-app
#   ./deploy/descargar-bd-produccion.sh
#
# El archivo queda en: backups/gym_produccion_YYYY-MM-DD_HHMMSS.sql.gz

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VPS_HOST="${VPS_HOST:-72.61.65.92}"
VPS_USER="${VPS_USER:-root}"
VPS_APP_DIR="${VPS_APP_DIR:-/apps/gym-app}"
LOCAL_DIR="${LOCAL_DIR:-$ROOT/backups}"
STAMP="$(date +%Y-%m-%d_%H%M%S)"
OUT="$LOCAL_DIR/gym_produccion_${STAMP}.sql.gz"

mkdir -p "$LOCAL_DIR"

echo "==> Conectando a ${VPS_USER}@${VPS_HOST}..."
echo "==> Volcando PostgreSQL desde Docker (puede tardar un minuto)..."

ssh "${VPS_USER}@${VPS_HOST}" bash -s <<'REMOTE' | gzip > "$OUT"
set -euo pipefail
docker exec gym-postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-acl'
REMOTE

echo "OK: $OUT"
ls -lh "$OUT"
echo ""
echo "Restaurar en local:"
echo "  ./deploy/restaurar-bd-local.sh $OUT"
