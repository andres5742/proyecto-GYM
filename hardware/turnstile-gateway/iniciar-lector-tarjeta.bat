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
  echo.
  call "%~dp0descargar-lector-recepcion.bat"
  if not exist "serial_card_reader.py" (
    echo.
    echo ERROR: No se pudo obtener serial_card_reader.py
    echo Copie TODA la carpeta turnstile-gateway a este PC
    echo o ejecute descargar-lector-recepcion.bat con internet.
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

set ACCESS_DEVICE_KEY=clave-torniquete-produccion-2026
set GYM_ACCESS_API=https://sportgymr10.com/api/access/zkt/event
set SERIAL_PORT=COM3
set SERIAL_BAUD=9600
REM hex = UID del chip (ej. A5AD8AE2), como el sistema anterior.
REM decimal = solo numeros (ej. 2198114). Solo si ZKAccess muestra solo numeros.
set SERIAL_PIN_FORMAT=hex
set SERIAL_DEBUG=0

REM Seguro torniquete: edite turnstile-gate.env (copie .example) o variables aqui:
REM Seguro: protocolo ATP-ACCESO (copie turnstile-gate.env.example -> turnstile-gate.env)
REM Cierre ATP-ACCESO 4.0.exe antes de abrir este lector (mismo COM3).
set TURNSTILE_GATE_MODE=serial
set TURNSTILE_GATE_PROTOCOL=atp-acceso
set TURNSTILE_GATE_PORT=COM3
set TURNSTILE_GATE_BAUD=19200
set TURNSTILE_LOCK_CHAR=d
set TURNSTILE_UNLOCK_CHAR=a
set TURNSTILE_UNLOCK_MS=8000

echo Puerto lector: %SERIAL_PORT%  Velocidad: %SERIAL_BAUD%
echo Seguro torniquete: %TURNSTILE_GATE_MODE% en %TURNSTILE_GATE_PORT%
echo API: %GYM_ACCESS_API%
echo.
echo Pase una tarjeta en el lector...
echo Si no aparece numero: iniciar-lector-debug.bat o iniciar-lector-115200.bat
echo.

python serial_card_reader.py

echo.
pause
