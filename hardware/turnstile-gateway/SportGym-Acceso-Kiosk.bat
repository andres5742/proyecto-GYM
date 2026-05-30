@echo off
title Sport Gym - Kiosk Acceso
cd /d "%~dp0"

echo Modo navegador desactivado. Se abrira la app de escritorio.
echo.
if exist "%~dp0SportGym-Acceso-App.bat" (
  call "%~dp0SportGym-Acceso-App.bat"
  exit /b 0
)

echo ERROR: No se encontro SportGym-Acceso-App.bat
echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat para reinstalar.
pause
exit /b 1
