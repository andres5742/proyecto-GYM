@echo off
setlocal EnableDelayedExpansion
title Instalar Sport Gym Acceso
cd /d "%~dp0"
set "DEST=C:\SportGym"
set "LOG=%DEST%\INSTALAR-LOG.txt"
set "SETUP_NAME=SportGym-Acceso-Setup-1.0.0-win32.exe"
set "SETUP_URL=https://sportgymr10.com/downloads/%SETUP_NAME%"

mkdir "%DEST%" 2>nul
echo [%date% %time%] Inicio >> "%LOG%"

echo ========================================
echo  Sport Gym Acceso - Instalador
echo ========================================
echo.
echo Instala la app .exe con logo del gym.
echo Pantalla desde sportgymr10.com - cierre y abra para actualizar.
echo.
pause

REM --- Lector COM3: copiar local o descargar desde GitHub ---
mkdir "%DEST%\turnstile-gateway" 2>nul
if exist "%~dp0turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo Copiando lector desde git...
  xcopy /E /I /Y /Q "%~dp0turnstile-gateway\*" "%DEST%\turnstile-gateway\"
) else (
  echo Descargando lector desde GitHub...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$base='https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/turnstile-gateway/';" ^
    "$dir='%DEST%\turnstile-gateway';" ^
    "New-Item -ItemType Directory -Force -Path $dir | Out-Null;" ^
    "$files=@('serial_card_reader.py','turnstile_gate.py','turnstile-gate.env','turnstile-gate.env.example','iniciar-lector-tarjeta.bat','detener-lector-tarjeta.bat','SportGym-Acceso-App.bat','SportGym-Acceso-Kiosk.bat','descargar-lector-recepcion.bat','probar-letras-com3.bat','poner-seguro.bat','quitar-seguro.bat');" ^
    "foreach($f in $files){ try { Invoke-WebRequest -Uri ($base+$f) -OutFile (Join-Path $dir $f) -UseBasicParsing; Write-Host ('OK '+$f) } catch { Write-Host ('ERROR '+$f) -ForegroundColor Red } }"
)
if not exist "%DEST%\turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo ERROR: no se pudo obtener el lector. Revise internet o git pull.
  pause
  exit /b 1
)
echo Lector OK >> "%LOG%"

REM --- Buscar Setup.exe local ---
set "SETUP="
if exist "%~dp0%SETUP_NAME%" set "SETUP=%~dp0%SETUP_NAME%"
if not defined SETUP if exist "%~dp0access-desktop\dist-release\%SETUP_NAME%" (
  set "SETUP=%~dp0access-desktop\dist-release\%SETUP_NAME%"
)
if not defined SETUP (
  for /f "delims=" %%D in ('dir /b /ad "%~dp0access-desktop\dist-release*" 2^>nul') do (
    if exist "%~dp0access-desktop\%%D\%SETUP_NAME%" set "SETUP=%~dp0access-desktop\%%D\%SETUP_NAME%"
  )
)
if not defined SETUP if exist "%DEST%\%SETUP_NAME%" set "SETUP=%DEST%\%SETUP_NAME%"

REM --- Descargar Setup desde sportgymr10.com ---
if not defined SETUP (
  echo.
  echo Descargando instalador desde el servidor...
  echo   %SETUP_URL%
  echo Puede tardar varios minutos...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { Invoke-WebRequest -Uri '%SETUP_URL%' -OutFile '%DEST%\%SETUP_NAME%' -UseBasicParsing; exit 0 } catch { Write-Host $_.Exception.Message; exit 1 }"
  if exist "%DEST%\%SETUP_NAME%" set "SETUP=%DEST%\%SETUP_NAME%"
)

if not defined SETUP (
  echo.
  echo ERROR: no hay instalador local ni en el servidor.
  echo.
  echo En la PC de desarrollo:
  echo   1. hardware\COMPILAR-Y-PREPARAR-INSTALADOR.bat
  echo   2. hardware\SUBIR-SETUP-AL-SERVIDOR.bat
  echo Luego vuelva a ejecutar este instalador en la PC del torniquete.
  pause
  exit /b 1
)

echo.
echo Instalando Sport Gym Acceso.exe...
echo   !SETUP!
echo Setup: !SETUP! >> "%LOG%"
start /wait "" "!SETUP!"

REM --- Acceso directo ---
echo Creando acceso directo...
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "TARGET="
set "WORKDIR="
if exist "%DEST%\Sport Gym Acceso.exe" (
  set "TARGET=%DEST%\Sport Gym Acceso.exe"
  set "WORKDIR=%DEST%"
)
if not defined TARGET if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "TARGET=%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
  set "WORKDIR=%LOCALAPPDATA%\Programs\Sport Gym Acceso"
)
if not defined TARGET if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  set "TARGET=%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
  set "WORKDIR=%ProgramFiles%\Sport Gym Acceso"
)

if defined TARGET (
  set "PS_T=!TARGET!"
  set "PS_W=!WORKDIR!"
  powershell -NoProfile -Command "$w=New-Object -ComObject WScript.Shell;$s=$w.CreateShortcut('%LINK%');$s.TargetPath=$env:PS_T;$s.WorkingDirectory=$env:PS_W;$s.Description='Sport Gym Acceso';$s.Save()"
  echo.
  echo ========================================
  echo  LISTO
  echo  Escritorio: Sport Gym Acceso
  echo  App: !TARGET!
  echo  Lector: %DEST%\turnstile-gateway
  echo  Pantalla: sportgymr10.com
  echo ========================================
) else (
  echo.
  echo AVISO: complete la instalacion del Setup si la cancelo.
  echo Vuelva a ejecutar este .bat.
)

pause
exit /b 0
