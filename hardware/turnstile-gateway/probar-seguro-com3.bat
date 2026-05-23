@echo off
title Sport Gym - Probar seguro COM3 (ATP-ACCESO)
cd /d "%~dp0"

where python >nul 2>&1
if errorlevel 1 (
  echo Instale Python con "Add to PATH"
  pause
  exit /b 1
)

pip install pyserial -q

if not exist "turnstile_gate.py" (
  echo ERROR: Falta turnstile_gate.py en esta carpeta.
  pause
  exit /b 1
)

echo.
echo PROTOCOLO gym (confirmado):
echo   COM3 @ 19200 — BLOQUEAR: h e i  LIBERAR: a
echo.
echo CIERRE ATP-ACCESO 4.0.exe e iniciar-lector-tarjeta.bat antes de probar.
echo.
pause

set TURNSTILE_GATE_MODE=serial
set TURNSTILE_GATE_PROTOCOL=atp-acceso
set TURNSTILE_GATE_PORT=COM3
set TURNSTILE_GATE_BAUD=19200
set TURNSTILE_LOCK_CHARS=hi
set TURNSTILE_UNLOCK_CHAR=a
set TURNSTILE_UNLOCK_MS=8000

echo --- PONER seguro (h + i) ---
python turnstile_gate.py lock
timeout /t 3 >nul

echo --- QUITAR seguro (a) — empuje el torniquete; vuelve a bloquear solo ---
python turnstile_gate.py unlock
timeout /t 2 >nul

echo.
echo Si el torniquete se movio: copie turnstile-gate.env.example a turnstile-gate.env
echo Luego use solo iniciar-lector-tarjeta.bat (ATP cerrado).
echo.
pause
