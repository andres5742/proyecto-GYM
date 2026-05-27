@echo off
title Sport Gym - Cerrar puerta remota
setlocal
cd /d "%~dp0"

REM PC torniquete en la misma red LAN
set "TARGET_IP=192.168.1.9"
set "TARGET_PORT=8765"
set "DEVICE_KEY=clave-torniquete-produccion-2026"

echo ========================================
echo  Sport Gym - Cierre remoto torniquete
echo ========================================
echo Destino: %TARGET_IP%:%TARGET_PORT%
echo.

powershell -NoProfile -Command ^
  "try { Invoke-RestMethod -Method POST -Uri 'http://%TARGET_IP%:%TARGET_PORT%/gate/sync' -Headers @{ 'X-Device-Key'='%DEVICE_KEY%' } -ContentType 'application/json' -Body '{\"action\":\"lock\"}' -TimeoutSec 8 | Out-Null; Write-Host 'OK: seguro activado.' -ForegroundColor Green; exit 0 } catch { Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }"

if errorlevel 1 (
  echo.
  echo Revise que en el PC torniquete este corriendo iniciar-lector-tarjeta.bat
  echo y que el puerto 8765 responda en la red local.
  pause
  exit /b 1
)

echo.
pause
exit /b 0
