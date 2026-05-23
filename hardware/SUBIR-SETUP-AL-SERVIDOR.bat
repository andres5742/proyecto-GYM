@echo off
title Subir Setup al servidor sportgymr10.com
cd /d "%~dp0"

set "SETUP="
if exist "SportGym-Acceso-Setup-1.0.0-win32.exe" set "SETUP=SportGym-Acceso-Setup-1.0.0-win32.exe"
if not defined SETUP if exist "..\downloads\SportGym-Acceso-Setup-1.0.0-win32.exe" (
  set "SETUP=..\downloads\SportGym-Acceso-Setup-1.0.0-win32.exe"
)
if not defined SETUP if exist "access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe" (
  set "SETUP=access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe"
)
if not defined SETUP (
  for /f "delims=" %%D in ('dir /b /ad "access-desktop\dist-release*" 2^>nul') do (
    if exist "access-desktop\%%D\SportGym-Acceso-Setup-1.0.0-win32.exe" (
      set "SETUP=access-desktop\%%D\SportGym-Acceso-Setup-1.0.0-win32.exe"
    )
  )
)
if not defined SETUP (
  echo No se encontro SportGym-Acceso-Setup-1.0.0-win32.exe
  echo Ejecute primero: COMPILAR-Y-PREPARAR-INSTALADOR.bat
  pause
  exit /b 1
)

echo Subiendo %SETUP% al VPS...
echo.
scp "%SETUP%" root@72.61.65.92:/apps/gym-app/downloads/SportGym-Acceso-Setup-1.0.0-win32.exe
if errorlevel 1 (
  echo ERROR en scp. Compruebe SSH y la ruta en el VPS.
  pause
  exit /b 1
)

echo.
echo Publicando en sportgymr10.com/downloads/ ...
ssh root@72.61.65.92 "cd /apps/gym-app && chmod +x deploy/publicar-setup-acceso.sh && ./deploy/publicar-setup-acceso.sh"
if errorlevel 1 (
  echo ERROR al publicar en el contenedor nginx.
  pause
  exit /b 1
)

echo.
echo LISTO. En la PC del torniquete: git pull + INSTALAR-SPORT-GYM-ENTRADA.bat
pause
