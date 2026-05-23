@echo off
title Sport Gym Acceso
cd /d "%~dp0"

if exist "C:\SportGym\SportGym-Acceso.vbs" (
  start "" wscript.exe "C:\SportGym\SportGym-Acceso.vbs"
  exit /b 0
)

if exist "C:\SportGym\SportGym-Acceso-App.bat" (
  start "" "C:\SportGym\SportGym-Acceso-App.bat"
  exit /b 0
)

if exist "C:\SportGym\turnstile-gateway\SportGym-Acceso-App.bat" (
  start "" "C:\SportGym\turnstile-gateway\SportGym-Acceso-App.bat"
  exit /b 0
)

if exist "C:\SportGym\Sport Gym Acceso.exe" (
  start "" "C:\SportGym\Sport Gym Acceso.exe"
  exit /b 0
)

if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
  exit /b 0
)

echo No esta instalado. Ejecute primero: INSTALAR-SPORT-GYM-ENTRADA.bat
pause
