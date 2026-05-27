@echo off
title Sport Gym Acceso - Inicio completo
REM Delega en el lanzador VBS (sin ventana negra que se cierra al instante).

set "DEST=C:\SportGym"
if exist "%~dp0ABRIR-SPORT-GYM-ACCESO.vbs" set "DEST=%~dp0"
if not "%DEST:~-1%"=="\" set "DEST=%DEST%\"

if exist "%DEST%ABRIR-SPORT-GYM-ACCESO.vbs" (
  start "" wscript.exe "%DEST%ABRIR-SPORT-GYM-ACCESO.vbs"
  exit /b 0
)

set "GW=%DEST%turnstile-gateway"
if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  echo ERROR: Falta %GW%\iniciar-lector-tarjeta.bat
  echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
  pause
  exit /b 1
)

if exist "%GW%\preparar-com3.bat" call "%GW%\preparar-com3.bat"

set "APP="
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
)
if not defined APP if exist "%DEST%Sport Gym Acceso.exe" set "APP=%DEST%Sport Gym Acceso.exe"
if not defined APP if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
)

if defined APP (
  echo Abriendo Sport Gym Acceso...
  start "" "%APP%"
  exit /b 0
)

echo ERROR: No se encontro Sport Gym Acceso.exe.
echo Instale o re-instale la aplicacion de escritorio.
echo Sugerencia: ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
pause
exit /b 1
