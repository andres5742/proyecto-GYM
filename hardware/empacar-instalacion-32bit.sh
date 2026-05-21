#!/bin/bash
# Paquete de instalación manual para Windows 32 bits (no requiere Node en el gym)
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/SportGym-Entrada-Instalador-32bit"
ZIP="$ROOT/SportGym-Entrada-Instalador-32bit.zip"
EXE="$ROOT/access-desktop/dist/SportGym-Acceso-1.0.0-win32.exe"

rm -rf "$OUT" "$ZIP"
mkdir -p "$OUT/turnstile-gateway"

if [[ ! -f "$EXE" ]]; then
  echo "Falta $EXE — compile antes: cd access-desktop && npm run dist"
  exit 1
fi

cp "$EXE" "$OUT/SportGym-Acceso.exe"
cp -r "$ROOT/turnstile-gateway/"* "$OUT/turnstile-gateway/"
cp "$ROOT/access-desktop/FLUJO-DOS-PC.txt" "$OUT/"

cat > "$OUT/INSTALAR.bat" << 'BAT'
@echo off
title Instalar Sport Gym Acceso
cd /d "%~dp0"
set DEST=C:\SportGym

echo ========================================
echo  INSTALADOR Sport Gym - PC de entrada
echo  Windows 32 bits
echo ========================================
echo.
echo Se instalara en: %DEST%
echo.
pause

mkdir "%DEST%" 2>nul
echo Copiando archivos...
copy /Y "%~dp0SportGym-Acceso.exe" "%DEST%\SportGym-Acceso.exe"
xcopy /E /I /Y /Q "%~dp0turnstile-gateway" "%DEST%\turnstile-gateway\"

echo Creando acceso directo...
powershell -NoProfile -Command "$d=[Environment]::GetFolderPath('Desktop');$w=New-Object -ComObject WScript.Shell;$s=$w.CreateShortcut($d+'\Sport Gym Acceso.lnk');$s.TargetPath='%DEST%\SportGym-Acceso.exe';$s.WorkingDirectory='%DEST%';$s.Save()" 2>nul
if errorlevel 1 (
  echo No se pudo crear acceso directo. Use %DEST%\SportGym-Acceso.exe
)

echo.
echo ========================================
echo  INSTALACION COMPLETA
echo.
echo  Abra: Sport Gym Acceso (escritorio)
echo  o ejecute: %DEST%\SportGym-Acceso.exe
echo.
echo  Debe verse una 2da ventana "Lector tarjeta COM3"
echo  (Python 32 bits debe estar instalado)
echo ========================================
echo.
pause
BAT

cat > "$OUT/LEEME.txt" << 'TXT'
SPORT GYM — INSTALACION PC ENTRADA (32 bits)
==========================================

1. Copie esta carpeta completa al PC de entrada (USB).
2. Doble clic en INSTALAR.bat (como Administrador si Windows lo pide).
3. En el escritorio: "Sport Gym Acceso".
4. Instale Python 32 bits si no esta:
   https://www.python.org/downloads/windows/
   Marque "Add python to PATH".

El portable .exe NO es instalador: use INSTALAR.bat.

Recepcion: https://sportgymr10.com -> Acceso -> Ingresos
TXT

cd "$ROOT"
zip -r "$ZIP" "$(basename "$OUT")"
ls -lh "$ZIP"
echo "Listo: $ZIP"

chmod +x "$ROOT/empacar-instalacion-32bit.sh"
bash "$ROOT/empacar-instalacion-32bit.sh"