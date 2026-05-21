@echo off
title Sport Gym - Kiosk Acceso
cd /d "%~dp0"

set URL=https://sportgymr10.com/acceso

start "Sport Gym - Lector tarjeta COM3" cmd /k call "%~dp0iniciar-lector-tarjeta.bat"
timeout /t 5 /nobreak >nul

if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
  start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --kiosk %URL% --edge-kiosk-type=fullscreen
  goto :ok
)
if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
  start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" --kiosk %URL%
  goto :ok
)
start "" %URL%

:ok
echo Kiosk pantalla completa. Alt+F4 para salir.
