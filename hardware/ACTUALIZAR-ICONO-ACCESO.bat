@echo off
chcp 65001 >nul
set "DEST=C:\SportGym"
set "GIT_HW=https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware"
set "TLS=[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12"

mkdir "%DEST%" 2>nul

if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico"
if not exist "%DEST%\SportGym.ico" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "%TLS%; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%GIT_HW%/SportGym.ico' -OutFile '%DEST%\SportGym.ico' -UseBasicParsing"
)
if not exist "%DEST%\SportGym.ico" if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" (
  copy /Y "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" "%DEST%\SportGym.ico"
)
if not exist "%DEST%\SportGym.ico" (
  echo No se encontro SportGym.ico
  pause
  exit /b 1
)

call "%~dp0CREAR-ACCESO-DIRECTO-ESCRITORIO.bat"
exit /b 0
