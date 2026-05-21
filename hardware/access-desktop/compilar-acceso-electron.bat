@echo off
title Compilar Sport Gym Acceso (.exe)
cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
  echo Instale Node.js LTS desde https://nodejs.org/
  pause
  exit /b 1
)

if not exist "config.json" (
  echo Creando config.json desde ejemplo...
  copy /Y config.example.json config.json
  echo Edite config.json con su ACCESS_DEVICE_KEY y rutas.
)

echo Instalando dependencias (primera vez tarda varios minutos)...
call npm install
if errorlevel 1 (
  pause
  exit /b 1
)

echo Compilando ejecutable portable...
call npm run dist
if errorlevel 1 (
  pause
  exit /b 1
)

echo.
echo Listo. Ejecutable en: dist\SportGym-Acceso-1.0.0.exe
echo Copie config.json junto al .exe si lo mueve a otra carpeta.
echo.
pause
