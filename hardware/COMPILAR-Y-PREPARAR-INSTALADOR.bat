@echo off
setlocal EnableDelayedExpansion
title Compilar Sport Gym Acceso (.exe con logo)
cd /d "%~dp0"

echo ========================================
echo  Generar SportGym-Acceso-Setup-1.0.0-win32.exe
echo  La app carga pantalla del SERVIDOR (sportgymr10.com/acceso).
echo  Cambios en el servidor: cierre y vuelva a abrir la app.
echo ========================================
echo.

call "%~dp0access-desktop\compilar-instalador-win32.bat"
if errorlevel 1 exit /b 1

set "SETUP_SRC="
if exist "access-desktop\ultima-compilacion.txt" (
  set /p LAST_DIR=<access-desktop\ultima-compilacion.txt
  if exist "access-desktop\!LAST_DIR!\SportGym-Acceso-Setup-1.0.0-win32.exe" (
    set "SETUP_SRC=access-desktop\!LAST_DIR!\SportGym-Acceso-Setup-1.0.0-win32.exe"
  )
)
if not defined SETUP_SRC if exist "access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe" (
  set "SETUP_SRC=access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe"
)
if not defined SETUP_SRC (
  for /f "delims=" %%D in ('dir /b /ad /o-d "access-desktop\dist-release*" 2^>nul') do (
    if not defined SETUP_SRC if exist "access-desktop\%%D\SportGym-Acceso-Setup-1.0.0-win32.exe" (
      set "SETUP_SRC=access-desktop\%%D\SportGym-Acceso-Setup-1.0.0-win32.exe"
    )
  )
)
if not defined SETUP_SRC (
  echo ERROR: no se genero el instalador NSIS.
  pause
  exit /b 1
)

copy /Y "!SETUP_SRC!" "%~dp0SportGym-Acceso-Setup-1.0.0-win32.exe" >nul
echo.
echo LISTO. Instalador copiado a:
echo   %~dp0SportGym-Acceso-Setup-1.0.0-win32.exe
echo   origen: !SETUP_SRC!
echo.
echo Siguiente paso (una vez): SUBIR-SETUP-AL-SERVIDOR.bat
echo En la PC del torniquete: INSTALAR-SPORT-GYM-ENTRADA.bat
echo.
pause
exit /b 0
