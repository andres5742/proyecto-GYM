@echo off
title Sport Gym Acceso
cd /d "%~dp0"

if exist "C:\SportGym\ABRIR-SPORT-GYM-ACCESO.vbs" (
  start "" wscript.exe "C:\SportGym\ABRIR-SPORT-GYM-ACCESO.vbs"
  exit /b 0
)

if exist "C:\SportGym\SportGym-Acceso.vbs" (
  start "" wscript.exe "C:\SportGym\SportGym-Acceso.vbs"
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

echo No se encontro el lanzador ni el .exe de Sport Gym Acceso.
echo Ejecute: ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
pause
