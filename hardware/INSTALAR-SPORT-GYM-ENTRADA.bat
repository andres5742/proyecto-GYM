@echo off
title Instalar Sport Gym Acceso
cd /d "%~dp0"
set "DEST=C:\SportGym"

echo ========================================
echo  Sport Gym Acceso - Instalador
echo ========================================
echo.
echo La pantalla carga del SERVIDOR:
echo   https://sportgymr10.com/acceso
echo En este PC: lector COM3 + torniquete (Python).
echo.
echo Requisitos:
echo   - Internet
echo   - Python en PATH
echo   - Cierre ATP-ACCESO 4.0.exe
echo.
pause

if not exist "%~dp0turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo ERROR: Falta carpeta turnstile-gateway junto a este .bat
  pause
  exit /b 1
)

echo Copiando lector y torniquete a %DEST% ...
mkdir "%DEST%" 2>nul
mkdir "%DEST%\turnstile-gateway" 2>nul
xcopy /E /I /Y /Q "%~dp0turnstile-gateway\*" "%DEST%\turnstile-gateway\"

REM --- Instalador NSIS (si copio el Setup.exe aqui) ---
set "SETUP="
if exist "%~dp0SportGym-Acceso-Setup-1.0.0-win32.exe" set "SETUP=%~dp0SportGym-Acceso-Setup-1.0.0-win32.exe"
if not defined SETUP if exist "%~dp0access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe" (
  set "SETUP=%~dp0access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe"
)
if not defined SETUP (
  for /d %%D in ("%~dp0access-desktop\dist-release*") do (
    if exist "%%~D\SportGym-Acceso-Setup-1.0.0-win32.exe" set "SETUP=%%~D\SportGym-Acceso-Setup-1.0.0-win32.exe"
  )
)

if defined SETUP (
  echo Ejecutando instalador Electron...
  start /wait "" "%SETUP%"
  goto :shortcut_electron
)

REM --- Carpeta Electron ya compilada ---
set "SRC="
if exist "%~dp0access-desktop\dist-release\win-ia32-unpacked\Sport Gym Acceso.exe" (
  set "SRC=%~dp0access-desktop\dist-release\win-ia32-unpacked"
)
if not defined SRC (
  for /d %%D in ("%~dp0access-desktop\dist-release*") do (
    if exist "%%~D\win-ia32-unpacked\Sport Gym Acceso.exe" set "SRC=%%~D\win-ia32-unpacked"
  )
)
if not defined SRC if exist "%~dp0access-desktop\dist-build\win-ia32-unpacked\Sport Gym Acceso.exe" (
  set "SRC=%~dp0access-desktop\dist-build\win-ia32-unpacked"
)

if defined SRC (
  echo Copiando Sport Gym Acceso.exe ...
  xcopy /E /I /Y /Q "%SRC%\*" "%DEST%\"
  goto :shortcut_electron
)

REM --- Sin .exe: modo navegador (misma pantalla del servidor) ---
echo.
echo No hay .exe Electron en esta carpeta.
echo Instalando MODO NAVEGADOR + lector (funciona igual con el servidor).
echo.
REM App desde sportgymr10.com (ventana propia + logo) + lector COM3
copy /Y "%~dp0SportGym-Acceso.vbs" "%DEST%\SportGym-Acceso.vbs" >nul
copy /Y "%~dp0SportGym-Acceso-App.bat" "%DEST%\SportGym-Acceso-App.bat" >nul
copy /Y "%~dp0SportGym-Acceso-Kiosk.bat" "%DEST%\SportGym-Acceso-Kiosk.bat" >nul

set "APP_VBS=%DEST%\SportGym-Acceso.vbs"
set "GW_DIR=%DEST%\turnstile-gateway"
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
powershell -NoProfile -Command "$w=New-Object -ComObject WScript.Shell;$s=$w.CreateShortcut('%LINK%');$s.TargetPath='wscript.exe';$s.Arguments='\"\"%APP_VBS%\"\"';$s.WorkingDirectory='%DEST%';$s.Description='Sport Gym Acceso (servidor)';$s.Save()" 2>nul

echo.
echo ========================================
echo  LISTO en %DEST%
echo  Escritorio: Sport Gym Acceso
echo  App carga de https://sportgymr10.com/acceso (logo del gym)
echo  + lector tarjeta COM3 en ventana aparte
echo.
echo  Para .exe Electron: copie junto a este .bat:
echo    SportGym-Acceso-Setup-1.0.0-win32.exe
echo  y vuelva a ejecutar este instalador.
echo ========================================
pause
exit /b 0

:shortcut_electron
echo Creando acceso directo...
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "TARGET="
if exist "%DEST%\Sport Gym Acceso.exe" set "TARGET=%DEST%\Sport Gym Acceso.exe" & set "WORKDIR=%DEST%"
if not defined TARGET if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "TARGET=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
  set "WORKDIR=%LOCALAPPDATA%\Programs\Sport Gym Acceso"
)
if not defined TARGET if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "TARGET=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
  set "WORKDIR=%ProgramFiles%\Sport Gym Acceso"
)
if defined TARGET (
  powershell -NoProfile -Command "$w=New-Object -ComObject WScript.Shell;$s=$w.CreateShortcut('%LINK%');$s.TargetPath='%TARGET%';$s.WorkingDirectory='%WORKDIR%';$s.Description='Ingreso gym';$s.Save()" 2>nul
)

echo.
echo ========================================
echo  LISTO
echo  Abra Sport Gym Acceso en el escritorio.
echo ========================================
pause
