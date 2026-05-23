@echo off
title Sport Gym Acceso - Inicio completo
REM Pantalla del servidor + lector COM3 (use si la app sola no abre el lector)

set "DEST=C:\SportGym"
set "GW=%DEST%\turnstile-gateway"

if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  echo ERROR: Falta %GW%
  echo Ejecute primero INSTALAR-SPORT-GYM-ENTRADA.bat
  pause
  exit /b 1
)

echo Iniciando lector COM3...
start "Sport Gym - Lector tarjeta COM3" cmd /k call "%GW%\iniciar-lector-tarjeta.bat"
timeout /t 4 /nobreak >nul

set "APP="
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
)
if not defined APP if exist "%DEST%\Sport Gym Acceso.exe" set "APP=%DEST%\Sport Gym Acceso.exe"
if not defined APP if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
)

if not defined APP (
  echo ERROR: No esta instalado Sport Gym Acceso.exe
  pause
  exit /b 1
)

echo Abriendo Sport Gym Acceso...
start "" "%APP%"
echo.
echo Debe ver 2 ventanas: lector COM3 + pantalla del gym.
pause
