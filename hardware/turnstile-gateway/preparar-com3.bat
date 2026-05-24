@echo off
setlocal EnableDelayedExpansion
REM Cierra ATP y lectores viejos para liberar COM3 antes de Sport Gym.
cd /d "%~dp0"

echo [COM3] Cerrando ATP-ACCESO si esta abierto...
taskkill /IM "ATP-ACCESO 4.0.exe" /F >nul 2>&1
taskkill /IM "ATP-ACCESO.exe" /F >nul 2>&1
for /f "tokens=2 delims==" %%a in (
  'wmic process where "CommandLine like '%%ATP-ACCESO%%'" get ProcessId /value 2^>nul ^| find "="'
) do taskkill /PID %%a /F >nul 2>&1

echo [COM3] Deteniendo lector Sport Gym anterior (si hay)...
if exist ".lector-tarjeta.pid" (
  set /p PID=<.lector-tarjeta.pid
  taskkill /PID !PID! /F >nul 2>&1
  del /f /q ".lector-tarjeta.pid" >nul 2>&1
)
for /f "tokens=2 delims==" %%a in (
  'wmic process where "name='python.exe' and CommandLine like '%%serial_card_reader.py%%'" get ProcessId /value 2^>nul ^| find "="'
) do taskkill /PID %%a /F >nul 2>&1

echo [COM3] Esperando 2 segundos...
timeout /t 2 /nobreak >nul
endlocal
exit /b 0
