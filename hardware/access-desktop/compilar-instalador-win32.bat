@echo off
setlocal EnableDelayedExpansion
title Compilar INSTALADOR Sport Gym (32 bits)
cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
  echo Instale Node.js LTS desde https://nodejs.org/
  pause
  exit /b 1
)

if not exist "config.json" copy /Y config.example.json config.json

echo Cerrando procesos que bloquean app.asar...
taskkill /F /IM "Sport Gym Acceso.exe" >nul 2>&1
taskkill /F /IM electron.exe >nul 2>&1
timeout /t 2 >nul

for /f %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "BUILD_DIR=dist-release-%%T"
set "OUT=%CD%\%BUILD_DIR%"

echo.
echo Carpeta de salida (nueva, evita bloqueo app.asar):
echo   %BUILD_DIR%
echo.

echo Instalando dependencias...
call npm install
if errorlevel 1 pause & exit /b 1

echo.
echo Compilando instalador NSIS (Windows 32 bits)...
echo Incluye turnstile-gateway + lector dentro del instalador.
echo.
call npx electron-builder --win nsis --ia32 --config.directories.output=%BUILD_DIR%
if errorlevel 1 (
  echo.
  echo ERROR al compilar.
  echo 1. Cierre Sport Gym Acceso.exe si esta abierto
  echo 2. Cierre Cursor sobre access-desktop\dist-release
  echo 3. Vuelva a ejecutar este .bat
  pause
  exit /b 1
)

echo %BUILD_DIR%> ultima-compilacion.txt
echo.
echo LISTO. Instalador:
echo   %BUILD_DIR%\SportGym-Acceso-Setup-1.0.0-win32.exe
echo.
pause
exit /b 0
