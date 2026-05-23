@echo off
setlocal EnableDelayedExpansion
title Crear acceso directo Sport Gym Acceso
set "DEST=C:\SportGym"
set "LAUNCHER=%DEST%\INICIAR-ACCESO-COMPLETO.bat"
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "ICON=%DEST%\SportGym.ico"
set "GIT_HW=https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware"
set "TLS=[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12"

mkdir "%DEST%" 2>nul

if not exist "%LAUNCHER%" (
  if exist "%~dp0INICIAR-ACCESO-COMPLETO.bat" copy /Y "%~dp0INICIAR-ACCESO-COMPLETO.bat" "%LAUNCHER%" >nul
)
if not exist "%LAUNCHER%" (
  echo ERROR: Falta %LAUNCHER%
  echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
  pause
  exit /b 1
)

if not exist "%ICON%" (
  if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%ICON%" >nul
)
if not exist "%ICON%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "%TLS%; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%GIT_HW%/SportGym.ico' -OutFile '%ICON%' -UseBasicParsing"
)
if not exist "%ICON%" if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" (
  copy /Y "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" "%ICON%" >nul
)

del "%LINK%" 2>nul

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$w=New-Object -ComObject WScript.Shell; $s=$w.CreateShortcut('%LINK%');" ^
  "$s.TargetPath='%LAUNCHER%'; $s.WorkingDirectory='%DEST%';" ^
  "$s.Description='Sport Gym Acceso - lector + pantalla';" ^
  "if (Test-Path '%ICON%') { $s.IconLocation='%ICON%,0' } else { Write-Host 'AVISO: sin SportGym.ico' -ForegroundColor Yellow };" ^
  "$s.Save()"

echo.
echo LISTO: Sport Gym Acceso en el escritorio
echo   Abre: %LAUNCHER%
if exist "%ICON%" (
  echo   Icono gym: %ICON%
) else (
  echo   AVISO: descargue SportGym.ico con ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
)
echo.
pause
