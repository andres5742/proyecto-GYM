@echo off
title Sport Gym Acceso - Inicio completo
REM Pantalla + lector COM3. Cierra ATP antes. Con Electron no duplica el lector.

set "DEST=C:\SportGym"
set "GW=%DEST%\turnstile-gateway"
if exist "%~dp0turnstile-gateway\iniciar-lector-tarjeta.bat" (
  set "DEST=%~dp0"
  set "GW=%DEST%turnstile-gateway"
)

if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  echo ERROR: Falta %GW%
  echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
  pause
  exit /b 1
)

if exist "%GW%\preparar-com3.bat" call "%GW%\preparar-com3.bat"

set "APP="
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
)
if not defined APP if exist "%DEST%\Sport Gym Acceso.exe" set "APP=%DEST%\Sport Gym Acceso.exe"
if not defined APP if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "APP=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
)

if defined APP (
  echo Abriendo Sport Gym Acceso (abre lector COM3 automaticamente)...
  start "" "%APP%"
  echo Listo. Debe ver ventana del lector COM3 y la pantalla de acceso.
  exit /b 0
)

echo Iniciando lector COM3 (modo sin Electron)...
start "Sport Gym - Lector tarjeta COM3" cmd /k call "%GW%\iniciar-lector-tarjeta.bat"
timeout /t 4 /nobreak >nul

if exist "%GW%\SportGym-Acceso-App.bat" (
  call "%GW%\SportGym-Acceso-App.bat"
) else (
  start "" "https://sportgymr10.com/acceso"
)
echo Debe ver lector COM3 + pantalla /acceso.
exit /b 0
