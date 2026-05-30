@echo off
title Sport Gym - App Acceso
cd /d "%~dp0"

echo Abriendo Sport Gym Acceso (app de escritorio)...
echo.

if exist "%~dp0preparar-com3.bat" call "%~dp0preparar-com3.bat"

start "Sport Gym - Lector tarjeta COM3" /min cmd /k call "%~dp0iniciar-lector-tarjeta.bat"
timeout /t 8 /nobreak >nul
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
  goto :ok
)
if exist "C:\SportGym\Sport Gym Acceso.exe" (
  start "" "C:\SportGym\Sport Gym Acceso.exe"
  goto :ok
)
if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
  goto :ok
)
if exist "%ProgramFiles(x86)%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%ProgramFiles(x86)%\Sport Gym Acceso\Sport Gym Acceso.exe"
  goto :ok
)

echo ERROR: No se encontro Sport Gym Acceso.exe
echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat para instalar la app.
pause
exit /b 1

:ok
echo.
echo App abierta en modo escritorio.
echo Lector COM3 en ventana aparte.
exit /b 0
