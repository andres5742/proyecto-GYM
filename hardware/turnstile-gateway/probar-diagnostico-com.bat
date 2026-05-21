@echo off
title Sport Gym - Diagnostico COM
cd /d "%~dp0"

where python >nul 2>&1
if errorlevel 1 (
  echo Instale Python y marque "Add to PATH"
  pause
  exit /b 1
)

pip install pyserial -q

set SERIAL_PORT=COM3
echo.
echo Edite SERIAL_PORT en este .bat si su puerto no es COM3.
echo.
pause

python diagnostico_puerto.py
echo.
pause
