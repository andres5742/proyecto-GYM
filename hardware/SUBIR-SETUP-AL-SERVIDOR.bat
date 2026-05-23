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
echo Tamaño ~65 MB, puede tardar varios minutos...
echo.
ssh root@72.61.65.92 "mkdir -p /apps/gym-app/downloads"
if errorlevel 1 (
  echo ERROR en ssh. Compruebe conexion y contraseña.
  pause
  exit /b 1
)
scp "%SETUP%" root@72.61.65.92:/apps/gym-app/downloads/SportGym-Acceso-Setup-1.0.0-win32.exe
if errorlevel 1 (
  echo ERROR en scp. Compruebe SSH y la ruta en el VPS.
  pause
  exit /b 1
)

echo.
echo Publicando en sportgymr10.com/downloads/ ...
ssh root@72.61.65.92 "cd /apps/gym-app && git pull && chmod +x deploy/publicar-setup-acceso.sh 2>nul; docker exec gym-frontend mkdir -p /usr/share/nginx/html/downloads && docker cp /apps/gym-app/downloads/SportGym-Acceso-Setup-1.0.0-win32.exe gym-frontend:/usr/share/nginx/html/downloads/SportGym-Acceso-Setup-1.0.0-win32.exe && docker exec gym-frontend ls -lh /usr/share/nginx/html/downloads/"
if errorlevel 1 (
  echo ERROR al publicar en el contenedor nginx.
  pause
  exit /b 1
)

echo.
echo LISTO. En la PC del torniquete copie y ejecute:
echo   ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
echo (Descarga: GitHub + Setup + acceso directo con logo gym)
echo Ver hardware\LEEME-TORNIQUETE.txt
pause
