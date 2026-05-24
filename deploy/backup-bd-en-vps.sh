#!/bin/bash
# Ejecutar EN EL VPS (donde corre Docker del gym).
# Crea un volcado SQL en /backups/ (o en ./backups si no existe /backups).
#
#   cd /apps/gym-app
#   chmod +x deploy/backup-bd-en-vps.sh
#   ./deploy/backup-bd-en-vps.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STAMP="$(date +%Y-%m-%d_%H%M%S)"
OUT_DIR="${BACKUP_DIR:-/backups}"
mkdir -p "$OUT_DIR"

if ! docker ps --format '{{.Names}}' | grep -qx 'gym-postgres'; then
  echo "El contenedor gym-postgres no está en ejecución."
  exit 1
fi

POSTGRES_DB="$(docker exec gym-postgres printenv POSTGRES_DB)"
POSTGRES_USER="$(docker exec gym-postgres printenv POSTGRES_USER)"
OUT_FILE="$OUT_DIR/gym_${POSTGRES_DB}_${STAMP}.sql"

echo "==> Volcando $POSTGRES_DB (usuario $POSTGRES_USER)..."
docker exec gym-postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-acl' > "$OUT_FILE"

gzip -f "$OUT_FILE"
echo "OK: ${OUT_FILE}.gz"
ls -lh "${OUT_FILE}.gz"
