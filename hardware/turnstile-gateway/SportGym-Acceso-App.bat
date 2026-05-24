@echo off
title Sport Gym - App Acceso
cd /d "%~dp0"

set URL=https://sportgymr10.com/acceso

echo Abriendo aplicacion de acceso (ventana propia, sin barra del navegador)...
echo URL: %URL%
echo.

if exist "%~dp0preparar-com3.bat" call "%~dp0preparar-com3.bat"

start "Sport Gym - Lector tarjeta COM3" /min cmd /k call "%~dp0iniciar-lector-tarjeta.bat"
timeout /t 5 /nobreak >nul

set "PROFILE=%LOCALAPPDATA%\SportGymAcceso\EdgeProfile"
mkdir "%PROFILE%" 2>nul

REM Microsoft Edge (modo app = ventana propia con logo del gym desde el servidor)
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
  start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app=%URL% --window-size=1400,900 --disable-features=TranslateUI --user-data-dir="%PROFILE%"
  goto :ok
)
if exist "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" (
  start "" "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" --app=%URL% --window-size=1400,900 --disable-features=TranslateUI --user-data-dir="%PROFILE%"
  goto :ok
)

REM Google Chrome
if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
  start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" --app=%URL% --window-size=1400,900
  goto :ok
)
if exist "%LocalAppData%\Google\Chrome\Application\chrome.exe" (
  start "" "%LocalAppData%\Google\Chrome\Application\chrome.exe" --app=%URL% --window-size=1400,900
  goto :ok
)

echo No se encontro Edge ni Chrome. Abriendo navegador predeterminado...
start "" %URL%

:ok
echo.
echo App abierta desde %URL% (pantalla del servidor, logo Sport Gym).
echo Lector COM3 en ventana aparte. Para pantalla completa: SportGym-Acceso-Kiosk.bat
exit /b 0
