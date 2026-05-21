@echo off
title Sport Gym - App Acceso
cd /d "%~dp0"

set URL=https://sportgymr10.com/acceso

echo Abriendo aplicacion de acceso (ventana propia, sin barra del navegador)...
echo URL: %URL%
echo.

start "Sport Gym - Lector tarjeta COM3" cmd /k call "%~dp0iniciar-lector-tarjeta.bat"
timeout /t 5 /nobreak >nul

REM Microsoft Edge (modo app = parece programa instalado)
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
  start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app=%URL% --window-size=1400,900 --disable-features=TranslateUI
  goto :ok
)
if exist "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" (
  start "" "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" --app=%URL% --window-size=1400,900 --disable-features=TranslateUI
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
echo Listo. Para pantalla completa use SportGym-Acceso-Kiosk.bat
pause
