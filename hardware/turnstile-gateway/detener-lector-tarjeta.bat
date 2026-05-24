@echo off
setlocal EnableDelayedExpansion
title Sport Gym - Detener lector tarjeta
cd /d "%~dp0"

echo ========================================
echo  Detener lector y liberar COM3
echo  (para que otras apps usen el dispositivo)
echo ========================================
echo.

set DETENIDO=0

if exist ".lector-tarjeta.pid" (
  set /p PID=<.lector-tarjeta.pid
  echo Cerrando proceso del lector (PID !PID!)...
  taskkill /PID !PID! /F >nul 2>&1
  if not errorlevel 1 set DETENIDO=1
  del /f /q ".lector-tarjeta.pid" >nul 2>&1
)

echo Buscando python con serial_card_reader.py...
for /f "tokens=2 delims==" %%a in ('wmic process where "name='python.exe' and CommandLine like '%%serial_card_reader.py%%'" get ProcessId /value 2^>nul ^| find "="') do (
  echo Cerrando PID %%a...
  taskkill /PID %%a /F >nul 2>&1
  if not errorlevel 1 set DETENIDO=1
)

if "!DETENIDO!"=="1" (
  echo.
  echo Listo: lector detenido y puerto serie liberado.
  echo Puede abrir ZKAccess u otra aplicacion que use COM3.
) else (
  echo.
  echo No habia ningun lector Sport Gym en ejecucion.
  echo El puerto COM3 deberia estar libre para otras apps.
)

echo.
echo Nota: esto NO desinstala Python ni borra datos del gym.
echo Solo cierra el puente USB -^> API de Sport Gym.
echo.
if /i not "%~1"=="/silent" pause
endlocal
