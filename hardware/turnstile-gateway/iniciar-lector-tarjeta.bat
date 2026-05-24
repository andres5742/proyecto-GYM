@echo off
title Sport Gym - Lector tarjeta COM3
cd /d "%~dp0"

echo ========================================
echo  Lector tarjeta - Sport Gym
echo  Deje esta ventana ABIERTA
echo  Para liberar COM3: detener-lector-tarjeta.bat
echo ========================================
echo.
echo Carpeta: %CD%
echo.

if not exist "serial_card_reader.py" (
  echo Falta serial_card_reader.py. Descargando desde GitHub...
  call "%~dp0descargar-lector-recepcion.bat"
  if not exist "serial_card_reader.py" (
    echo ERROR: No se pudo obtener serial_card_reader.py
    pause
    exit /b 1
  )
)

where python >nul 2>&1
if errorlevel 1 (
  echo ERROR: Instale Python desde https://www.python.org/downloads/
  echo Marque "Add python to PATH" al instalar.
  pause
  exit /b 1
)

pip install pyserial -q

if exist "%~dp0preparar-com3.bat" call "%~dp0preparar-com3.bat"

if not exist "turnstile-gate.env" if exist "turnstile-gate.env.example" (
  copy /Y "turnstile-gate.env.example" "turnstile-gate.env" >nul
)

set ACCESS_DEVICE_KEY=clave-torniquete-produccion-2026
set GYM_ACCESS_API=https://sportgymr10.com/api/access/zkt/event
set SERIAL_PORT=COM3
set SERIAL_BAUD=9600
set SERIAL_PIN_FORMAT=hex
set SERIAL_DEBUG=0
set TURNSTILE_GATE_MODE=serial
set TURNSTILE_GATE_PROTOCOL=atp-acceso
set TURNSTILE_GATE_PORT=COM3
set TURNSTILE_GATE_BAUD=19200
set TURNSTILE_LOCK_CHARS=hi
set TURNSTILE_UNLOCK_CHAR=a
set TURNSTILE_UNLOCK_MS=8000
set GATE_HTTP_PORT=8765

echo Puerto lector: %SERIAL_PORT% @ %SERIAL_BAUD%
echo Seguro torniquete: %TURNSTILE_GATE_MODE% %TURNSTILE_LOCK_CHARS% en %TURNSTILE_GATE_PORT% @ %TURNSTILE_GATE_BAUD%
echo API: %GYM_ACCESS_API%
echo.
echo Bloqueo inicial del torniquete (h + i)...
python turnstile_gate.py startup
echo.
echo Al iniciar debe verse: SEGURO: PONER seguro ...
echo Pase una tarjeta. Solo se libera con acceso GRANTED activo.
echo.

python serial_card_reader.py

echo.
pause
