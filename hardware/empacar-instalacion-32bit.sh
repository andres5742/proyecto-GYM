#!/bin/bash
# Paquete Windows 32 bits: carpeta completa (no portable) + lector + INSTALAR.bat
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/SportGym-Entrada-Instalador-32bit"
ZIP="$ROOT/SportGym-Entrada-Instalador-32bit.zip"
APP_SRC="$ROOT/access-desktop/dist/win-ia32-unpacked"

echo "==> Limpiando paquete anterior..."
rm -rf "$OUT" "$ZIP"
mkdir -p "$OUT/turnstile-gateway"

if [[ ! -f "$APP_SRC/Sport Gym Acceso.exe" ]]; then
  echo "==> Compilando app (carpeta win-ia32-unpacked)..."
  (cd "$ROOT/access-desktop" && npm install && npm run dist)
fi

if [[ ! -f "$APP_SRC/Sport Gym Acceso.exe" ]]; then
  echo "ERROR: No se generó $APP_SRC/Sport Gym Acceso.exe"
  exit 1
fi

echo "==> Copiando aplicación completa..."
mkdir -p "$OUT/app"
cp -a "$APP_SRC/." "$OUT/app/"
cp -r "$ROOT/turnstile-gateway/." "$OUT/turnstile-gateway/"
cp "$ROOT/access-desktop/FLUJO-DOS-PC.txt" "$OUT/"
cp "$ROOT/CERRAR-ACCESO.bat" "$OUT/"
cp "$ROOT/ACTUALIZAR-ICONO-ACCESO.bat" "$OUT/" 2>/dev/null || true
if [[ -f "$ROOT/access-desktop/build/icon.ico" ]]; then
  cp "$ROOT/access-desktop/build/icon.ico" "$OUT/SportGym.ico"
elif [[ -f "$APP_SRC/resources/SportGym.ico" ]]; then
  cp "$APP_SRC/resources/SportGym.ico" "$OUT/SportGym.ico"
fi

cat > "$OUT/INSTALAR.bat" << 'BAT'
@echo off
chcp 65001 >nul
title Instalar Sport Gym Acceso
cd /d "%~dp0"
set "DEST=C:\SportGym"
set "LOG=%DEST%\instalacion.log"

echo [%date% %time%] Inicio > "%LOG%" 2>nul
if not exist "%DEST%" mkdir "%DEST%"

echo.
echo  ============================================
echo   INSTALADOR Sport Gym - PC de entrada
echo   Windows 32 bits
echo  ============================================
echo.
echo  Instalara en: %DEST%
echo  (copia todos los archivos de la app, no solo un .exe)
echo.
pause

if not exist "%~dp0app\Sport Gym Acceso.exe" (
  echo ERROR: Falta la carpeta "app" con Sport Gym Acceso.exe
  echo Copie el ZIP completo y descomprima todo.
  pause
  exit /b 1
)

echo Copiando aplicacion...
robocopy "%~dp0app" "%DEST%" /E /NFL /NDL /NJH /NJS /nc /ns /np
if errorlevel 8 goto :error

echo Copiando lector de tarjetas...
if not exist "%DEST%\turnstile-gateway" mkdir "%DEST%\turnstile-gateway"
robocopy "%~dp0turnstile-gateway" "%DEST%\turnstile-gateway" /E /NFL /NDL /NJH /NJS /nc /ns /np
if errorlevel 8 goto :error

copy /Y "%~dp0CERRAR-ACCESO.bat" "%DEST%\CERRAR-ACCESO.bat" >nul

if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico" >nul
if exist "%DEST%\resources\SportGym.ico" copy /Y "%DEST%\resources\SportGym.ico" "%DEST%\SportGym.ico" >nul

echo Creando acceso directo...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$d=[Environment]::GetFolderPath('Desktop');" ^
  "$w=New-Object -ComObject WScript.Shell;" ^
  "$s=$w.CreateShortcut($d+'\Sport Gym Acceso.lnk');" ^
  "$s.TargetPath='%DEST%\Sport Gym Acceso.exe';" ^
  "$s.WorkingDirectory='%DEST%';" ^
  "$s.Description='Ingreso Sport Gym';" ^
  "$icon='%DEST%\SportGym.ico';" ^
  "if (Test-Path $icon) { $s.IconLocation = $icon + ',0' };" ^
  "$s.Save()"

echo [%date% %time%] OK >> "%LOG%"

echo.
echo  INSTALACION COMPLETA
echo  Abriendo Sport Gym Acceso...
echo.
start "" "%DEST%\Sport Gym Acceso.exe"
timeout /t 3 >nul
pause
exit /b 0

:error
echo ERROR en la copia. Ejecute como Administrador.
echo [%date% %time%] ERROR >> "%LOG%" 2>nul
pause
exit /b 1
BAT

cat > "$OUT/ABRIR-SIN-INSTALAR.bat" << 'BAT'
@echo off
cd /d "%~dp0app"
if not exist "Sport Gym Acceso.exe" (
  echo Falta carpeta app. Descomprima el ZIP completo.
  pause
  exit /b 1
)
start "" "Sport Gym Acceso.exe"
BAT

cat > "$OUT/LEEME.txt" << 'TXT'
SPORT GYM - PC DE ENTRADA (Windows 32 bits)
============================================

1. Copie TODO el ZIP al PC (USB).
2. Descomprima la carpeta completa.
3. Doble clic en INSTALAR.bat
   (instala en C:\SportGym y abre la pantalla).

Prueba rapida sin instalar: ABRIR-SIN-INSTALAR.bat

Requisitos:
- Windows 7 o superior (32 bits)
- Internet para cargar sportgymr10.com/acceso
- Python 32 bits con "Add to PATH" (lector tarjeta)

NO ejecute solo un .exe suelto: use INSTALAR.bat

Cerrar la app: Alt+F4, Ctrl+Shift+Q, o CERRAR-ACCESO.bat
TXT

echo "==> Creando ZIP (puede tardar 1-2 min)..."
(cd "$ROOT" && zip -rq "$ZIP" "$(basename "$OUT")")

echo ""
echo "LISTO:"
echo "  $ZIP"
ls -lh "$ZIP"
du -sh "$OUT"
