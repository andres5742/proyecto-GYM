@echo off
title Sport Gym - Instalar inicio automatico
cd /d "%~dp0"

set STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set LINK=%STARTUP%\SportGym-Puesto-Acceso.bat

echo Copiando inicio automatico a:
echo %STARTUP%
echo.

copy /Y "%~dp0iniciar-puesto-acceso.bat" "%LINK%" >nul
if errorlevel 1 (
  echo ERROR: no se pudo copiar. Ejecute como usuario normal con permisos en Startup.
  pause
  exit /b 1
)

echo OK. Al iniciar sesion en Windows se abrira:
echo   1. Lector de tarjeta (COM3)
echo   2. Navegador en https://sportgymr10.com/acceso
echo.
echo Para quitar: ejecute quitar-inicio-automatico.bat
echo.
pause
