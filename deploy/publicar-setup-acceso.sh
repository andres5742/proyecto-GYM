#!/bin/bash
# Publica el instalador Sport Gym Acceso en sportgymr10.com/downloads/
# Uso en el VPS (después de copiar el Setup.exe al servidor):
#   cd /apps/gym-app
#   mkdir -p downloads
#   # subir SportGym-Acceso-Setup-1.0.0-win32.exe a downloads/
#   ./deploy/publicar-setup-acceso.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SETUP_NAME="SportGym-Acceso-Setup-1.0.0-win32.exe"
SRC="${1:-$ROOT/downloads/$SETUP_NAME}"
CONTAINER="${GYM_FRONTEND_CONTAINER:-gym-frontend}"
DEST="/usr/share/nginx/html/downloads"

if [ ! -f "$SRC" ]; then
  echo "ERROR: no existe $SRC"
  echo "Copie el Setup compilado a: $ROOT/downloads/$SETUP_NAME"
  echo "O: $0 /ruta/al/$SETUP_NAME"
  exit 1
fi

echo "==> Crear carpeta downloads en contenedor $CONTAINER..."
docker exec "$CONTAINER" mkdir -p "$DEST"

echo "==> Copiar instalador (~70-150 MB, puede tardar)..."
docker cp "$SRC" "$CONTAINER:$DEST/$SETUP_NAME"

echo "==> Verificar..."
docker exec "$CONTAINER" ls -lh "$DEST/$SETUP_NAME"

echo ""
echo "OK. Descarga disponible en:"
echo "  https://sportgymr10.com/downloads/$SETUP_NAME"
echo ""
echo "En la PC del torniquete: git pull y ejecute INSTALAR-SPORT-GYM-ENTRADA.bat"
