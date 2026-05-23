@echo off
REM Lanzador en C:\SportGym: el lector vive en turnstile-gateway\
cd /d "%~dp0"
if not exist "%~dp0turnstile-gateway\SportGym-Acceso-Kiosk.bat" (
  echo ERROR: Falta carpeta turnstile-gateway en %~dp0
  echo Ejecute de nuevo: INSTALAR-SPORT-GYM-ENTRADA.bat
  pause
  exit /b 1
)
call "%~dp0turnstile-gateway\SportGym-Acceso-Kiosk.bat"
