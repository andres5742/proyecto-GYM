#!/bin/bash
# Restaura un volcado (.sql o .sql.gz) en PostgreSQL local (docker compose).
#
#   docker compose up -d          # postgres local
#   ./deploy/restaurar-bd-local.sh backups/gym_produccion_....sql.gz
#
# ADVERTENCIA: borra y recrea el esquema public de gym_db local.

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DUMP="${1:-}"

if [[ -z "$DUMP" || ! -f "$DUMP" ]]; then
  echo "Uso: $0 <archivo.sql o archivo.sql.gz>"
  exit 1
fi

DB_NAME="${DB_NAME:-gym_db}"
DB_USER="${DB_USER:-gym_user}"

cd "$ROOT"
docker compose up -d postgres

echo "==> Esperando PostgreSQL local..."
until docker exec gym-postgres pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; do
  sleep 1
done

echo "==> Reiniciando base local $DB_NAME (se pierden datos locales actuales)..."
docker exec gym-postgres psql -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" \
  2>/dev/null || true
docker exec gym-postgres psql -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS ${DB_NAME};"
docker exec gym-postgres psql -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE ${DB_NAME};"

echo "==> Importando $DUMP ..."
if [[ "$DUMP" == *.gz ]]; then
  gunzip -c "$DUMP" | docker exec -i gym-postgres psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q
else
  docker exec -i gym-postgres psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q < "$DUMP"
fi

echo "OK. Base local restaurada. Reinicie el backend si estaba en marcha."
