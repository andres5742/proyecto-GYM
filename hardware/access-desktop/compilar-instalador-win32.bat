@echo off
title Compilar INSTALADOR Sport Gym (32 bits)
cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
  echo Instale Node.js LTS 32 o 64 bits desde https://nodejs.org/
  pause
  exit /b 1
)

if not exist "config.json" copy /Y config.example.json config.json

echo Instalando dependencias...
call npm install
if errorlevel 1 pause & exit /b 1

echo.
echo Compilando instalador NSIS (Windows 32 bits)...
echo Incluye turnstile-gateway + lector dentro del instalador.
echo.
call npm run dist:install
if errorlevel 1 (
  echo.
  echo Si falla, use en la carpeta hardware:
  echo   INSTALAR-SPORT-GYM-ENTRADA.bat
  pause
  exit /b 1
)

echo.
echo LISTO. Ejecute en el PC de entrada:
echo   dist\SportGym-Acceso-Setup-1.0.0-win32.exe
echo.
pause
