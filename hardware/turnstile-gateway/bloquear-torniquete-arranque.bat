@echo off
setlocal EnableDelayedExpansion
title Sport Gym - Bloqueo torniquete al arranque
cd /d "%~dp0"

set "TURNSTILE_GATE_PORT=%TURNSTILE_GATE_PORT%"
if not defined TURNSTILE_GATE_PORT set "TURNSTILE_GATE_PORT=COM3"
set "TURNSTILE_GATE_BAUD=%TURNSTILE_GATE_BAUD%"
if not defined TURNSTILE_GATE_BAUD set "TURNSTILE_GATE_BAUD=19200"
set "TURNSTILE_LOCK_CHARS=%TURNSTILE_LOCK_CHARS%"
if not defined TURNSTILE_LOCK_CHARS set "TURNSTILE_LOCK_CHARS=hil"
set "LOCK_RETRIES=20"
set "LOCK_WAIT_MS=900"

echo [TORNIQUETE] Bloqueo de arranque en %TURNSTILE_GATE_PORT% @ %TURNSTILE_GATE_BAUD%
echo [TORNIQUETE] Secuencia bloqueo: %TURNSTILE_LOCK_CHARS%

for /L %%I in (1,1,%LOCK_RETRIES%) do (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$p='%TURNSTILE_GATE_PORT%'; $b=%TURNSTILE_GATE_BAUD%; $chars='%TURNSTILE_LOCK_CHARS%';" ^
    "try {" ^
    "  $sp = New-Object System.IO.Ports.SerialPort($p,$b,[System.IO.Ports.Parity]::None,8,[System.IO.Ports.StopBits]::One);" ^
    "  $sp.ReadTimeout = 500; $sp.WriteTimeout = 500; $sp.Open();" ^
    "  $sp.Write($chars); Start-Sleep -Milliseconds 180; $sp.Write('h');" ^
    "  $sp.Close(); exit 0" ^
    "} catch { exit 1 }"
  if not errorlevel 1 (
    echo [TORNIQUETE] Bloqueado (intento %%I/%LOCK_RETRIES%).
    exit /b 0
  )
  echo [TORNIQUETE] Reintentando bloqueo %%I/%LOCK_RETRIES%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Milliseconds %LOCK_WAIT_MS%"
)

echo [TORNIQUETE] AVISO: no se pudo bloquear tras %LOCK_RETRIES% intentos.
exit /b 1
