@echo off
title Compilar INSTALADOR Sport Gym (32 bits)
cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
  echo Instale Node.js LTS desde https://nodejs.org/
  pause
  exit /b 1
)

if not exist "config.json" copy /Y config.example.json config.json

echo Cerrando Sport Gym Acceso si esta abierto (libera app.asar)...
taskkill /F /IM "Sport Gym Acceso.exe" >nul 2>&1
taskkill /F /IM electron.exe >nul 2>&1
timeout /t 2 >nul

REM dist/ suele quedar bloqueada por Cursor o una ejecucion anterior.
if exist "dist-release" (
  echo Moviendo compilacion anterior...
  if exist "dist-release-old" rmdir /S /Q "dist-release-old" 2>nul
  move /Y "dist-release" "dist-release-old" >nul 2>&1
)

echo Instalando dependencias...
call npm install
if errorlevel 1 pause & exit /b 1

echo.
echo Compilando instalador NSIS (Windows 32 bits)...
echo Incluye turnstile-gateway + lector dentro del instalador.
echo.
call npx electron-builder --win nsis --ia32 --config.directories.output=dist-release
if errorlevel 1 (
  echo.
  echo ERROR: app.asar en uso. Cierre Sport Gym Acceso.exe y Cursor sobre dist\
  echo Luego borre manualmente dist\ y dist-build\ si existen, y reintente.
  pause
  exit /b 1
)

echo.
echo LISTO. Instalador:
echo   dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe
echo.
pause
