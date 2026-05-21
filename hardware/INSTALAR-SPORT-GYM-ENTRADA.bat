@echo off
title Instalar Sport Gym Acceso (sin asistente Electron)
cd /d "%~dp0"

set DEST=C:\SportGym
echo Instalando en %DEST% ...
echo.

if not exist "access-desktop\dist\SportGym-Acceso-1.0.0-win32.exe" (
  if exist "SportGym-Acceso-1.0.0-win32.exe" (
    set EXE=%~dp0SportGym-Acceso-1.0.0-win32.exe
  ) else (
    echo Falta SportGym-Acceso-1.0.0-win32.exe en esta carpeta.
    echo Copie el ZIP completo del proyecto hardware.
    pause
    exit /b 1
  )
) else (
  set EXE=%~dp0access-desktop\dist\SportGym-Acceso-1.0.0-win32.exe
)

if not exist "turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo Falta la carpeta turnstile-gateway
  pause
  exit /b 1
)

mkdir "%DEST%" 2>nul
mkdir "%DEST%\turnstile-gateway" 2>nul

echo Copiando aplicacion...
copy /Y "%EXE%" "%DEST%\SportGym-Acceso.exe"
xcopy /E /I /Y /Q "%~dp0turnstile-gateway\*" "%DEST%\turnstile-gateway\"

echo.
echo Creando acceso directo en el escritorio...
powershell -NoProfile -Command "$w=New-Object -ComObject WScript.Shell;$s=$w.CreateShortcut([Environment]::GetFolderPath('Desktop')+'\Sport Gym Acceso.lnk');$s.TargetPath='%DEST%\SportGym-Acceso.exe';$s.WorkingDirectory='%DEST%';$s.Description='Ingreso gym';$s.Save()" 2>nul

echo.
echo ========================================
echo  INSTALADO en %DEST%
echo  Ejecute: Sport Gym Acceso (escritorio)
echo  o %DEST%\SportGym-Acceso.exe
echo ========================================
echo.
echo Requisito: Python 32 bits instalado en el PC.
echo.
pause
