@echo off
title Instalar Sport Gym Acceso
cd /d "%~dp0"

echo ========================================
echo  Sport Gym Acceso - Instalador
echo ========================================
echo.
echo La app carga la pantalla desde el SERVIDOR:
echo   https://sportgymr10.com/acceso
echo En este PC solo queda el lector COM3 + torniquete.
echo.
echo Requisitos en el PC de entrada:
echo   - Internet
echo   - Python (Add to PATH) para lector de tarjeta
echo   - Cierre ATP-ACCESO 4.0.exe antes de usar
echo.
pause

REM --- Opcion 1: instalador NSIS (recomendado) ---
if exist "access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe" (
  echo Ejecutando instalador...
  start /wait "" "access-desktop\dist-release\SportGym-Acceso-Setup-1.0.0-win32.exe"
  goto :shortcut
)

for /d %%D in ("access-desktop\dist-release*") do (
  if exist "%%~D\SportGym-Acceso-Setup-1.0.0-win32.exe" (
    echo Ejecutando instalador en %%~D ...
    start /wait "" "%%~D\SportGym-Acceso-Setup-1.0.0-win32.exe"
    goto :shortcut
  )
)

REM --- Opcion 2: copiar carpeta ya compilada (sin asistente) ---
set DEST=C:\SportGym
set SRC=

if exist "access-desktop\dist-release\win-ia32-unpacked\Sport Gym Acceso.exe" (
  set SRC=access-desktop\dist-release\win-ia32-unpacked
)
if not defined SRC (
  for /d %%D in ("access-desktop\dist-release*") do (
    if exist "%%~D\win-ia32-unpacked\Sport Gym Acceso.exe" (
      set SRC=%%~D\win-ia32-unpacked
      goto :copy
    )
  )
)
if exist "access-desktop\dist-build\win-ia32-unpacked\Sport Gym Acceso.exe" (
  set SRC=access-desktop\dist-build\win-ia32-unpacked
)

:copy
if not defined SRC (
  echo.
  echo No se encontro el instalador ni la carpeta compilada.
  echo Primero ejecute en access-desktop:
  echo   compilar-instalador-win32.bat
  echo.
  pause
  exit /b 1
)

echo Copiando app a %DEST% ...
mkdir "%DEST%" 2>nul
xcopy /E /I /Y /Q "%SRC%\*" "%DEST%\"

if not exist "%DEST%\turnstile-gateway\iniciar-lector-tarjeta.bat" (
  if exist "turnstile-gateway\iniciar-lector-tarjeta.bat" (
    xcopy /E /I /Y /Q "turnstile-gateway\*" "%DEST%\turnstile-gateway\"
  )
)

:shortcut
echo.
echo Creando acceso directo en el escritorio...
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "TARGET="
if exist "C:\SportGym\Sport Gym Acceso.exe" set "TARGET=C:\SportGym\Sport Gym Acceso.exe" & set "WORKDIR=C:\SportGym"
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
echo  Abra "Sport Gym Acceso" en el escritorio.
echo  Debe cargar https://sportgymr10.com/acceso
echo ========================================
echo.
pause
