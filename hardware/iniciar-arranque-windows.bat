@echo off
REM Arranque al encender Windows o al iniciar sesion (Sport Gym torniquete).
REM Orden: cerrar ATP -^> liberar COM3 -^> lector -^> pantalla Electron (o Edge).

setlocal
title Sport Gym - Arranque

set "DEST=C:\SportGym"
if exist "%~dp0ABRIR-SPORT-GYM-ACCESO.vbs" set "DEST=%~dp0"
if not "%DEST:~-1%"=="\" set "DEST=%DEST%\"
set "GW=%DEST%turnstile-gateway"

if exist "%DEST%ABRIR-SPORT-GYM-ACCESO.vbs" (
  start "" wscript.exe "%DEST%ABRIR-SPORT-GYM-ACCESO.vbs"
  exit /b 0
)

if exist "%~dp0turnstile-gateway\preparar-com3.bat" (
  set "DEST=%~dp0"
  if not "%DEST:~-1%"=="\" set "DEST=%DEST%\"
  set "GW=%DEST%turnstile-gateway"
)

if not exist "%GW%\preparar-com3.bat" (
  echo ERROR: Falta %GW%
  echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat en este PC.
  timeout /t 15
  exit /b 1
)

call "%GW%\preparar-com3.bat"

set "APP="
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
)
if not defined APP if exist "%DEST%\Sport Gym Acceso.exe" set "APP=%DEST%\Sport Gym Acceso.exe"
if not defined APP if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
)

if defined APP (
  echo Abriendo Sport Gym Acceso (lector COM3 + pantalla)...
  start "" "%APP%"
  exit /b 0
)

if exist "%DEST%\INICIAR-ACCESO-COMPLETO.bat" (
  call "%DEST%\INICIAR-ACCESO-COMPLETO.bat"
  exit /b 0
)

if exist "%GW%\SportGym-Acceso-App.bat" (
  call "%GW%\SportGym-Acceso-App.bat"
  exit /b 0
)

echo ERROR: Instale Sport Gym Acceso desde sportgymr10.com/downloads/
timeout /t 20
exit /b 1
