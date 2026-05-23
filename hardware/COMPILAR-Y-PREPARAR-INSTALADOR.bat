@echo off
title Compilar Sport Gym Acceso (.exe con logo)
cd /d "%~dp0access-desktop"

echo ========================================
echo  Generar SportGym-Acceso-Setup-1.0.0-win32.exe
echo  La app carga pantalla del SERVIDOR (sportgymr10.com/acceso).
echo  Cambios en el servidor: cierre y vuelva a abrir la app.
echo ========================================
echo.

call "%~dp0access-desktop\compilar-instalador-win32.bat"
if errorlevel 1 exit /b 1

set "SETUP_SRC="
if exist "dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe" (
  set "SETUP_SRC=dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe"
)
if not defined SETUP_SRC (
  for /d %%D in ("dist-release*") do (
    if exist "%%~D\SportGym-Acceso-Setup-1.0.0-win32.exe" set "SETUP_SRC=%%~D\SportGym-Acceso-Setup-1.0.0-win32.exe"
  )
)
if not defined SETUP_SRC (
  echo ERROR: no se genero el instalador NSIS.
  pause
  exit /b 1
)

copy /Y "%SETUP_SRC%" "%~dp0SportGym-Acceso-Setup-1.0.0-win32.exe" >nul
echo.
echo LISTO. Instalador copiado a:
echo   %~dp0SportGym-Acceso-Setup-1.0.0-win32.exe
echo.
echo En la PC de entrada ejecute:
echo   INSTALAR-SPORT-GYM-ENTRADA.bat
echo.
pause
