@echo off
setlocal EnableDelayedExpansion
title Crear acceso directo Sport Gym Acceso
set "DEST=C:\SportGym"
set "LAUNCHER=%DEST%\INICIAR-ACCESO-COMPLETO.bat"
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"

if not exist "%LAUNCHER%" (
  if exist "%~dp0INICIAR-ACCESO-COMPLETO.bat" (
    mkdir "%DEST%" 2>nul
    copy /Y "%~dp0INICIAR-ACCESO-COMPLETO.bat" "%LAUNCHER%" >nul
  )
)
if not exist "%LAUNCHER%" (
  echo ERROR: Falta %LAUNCHER%
  echo Ejecute primero INSTALAR-SPORT-GYM-ENTRADA.bat
  pause
  exit /b 1
)

set "ICON="
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "ICON=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe,0"
)
if not defined ICON if exist "%DEST%\Sport Gym Acceso.exe" (
  set "ICON=%DEST%\Sport Gym Acceso.exe,0"
)
if not defined ICON if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "ICON=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe,0"
)

set "PS_ICON=!ICON!"
powershell -NoProfile -Command ^
  "$w=New-Object -ComObject WScript.Shell;" ^
  "$s=$w.CreateShortcut('%LINK%');" ^
  "$s.TargetPath='%LAUNCHER%';" ^
  "$s.WorkingDirectory='%DEST%';" ^
  "$s.Description='Sport Gym Acceso - lector + pantalla';" ^
  "if($env:PS_ICON){$s.IconLocation=$env:PS_ICON};" ^
  "$s.Save()"

echo.
echo LISTO: acceso directo en el escritorio
echo   Nombre: Sport Gym Acceso
echo   Abre: lector COM3 + app (logo del gym)
echo   Destino: %LAUNCHER%
if defined ICON echo   Icono: !ICON!
echo.
pause
