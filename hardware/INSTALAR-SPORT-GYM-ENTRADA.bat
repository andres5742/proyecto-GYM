@echo off
setlocal EnableDelayedExpansion
title Instalar Sport Gym Acceso
REM ============================================================
REM  SOLO COPIE ESTE ARCHIVO al PC del torniquete y ejecutelo.
REM  Descarga e instala todo: app .exe + lector COM3 + acceso directo.
REM  Pantalla desde sportgymr10.com - cierre y abra para actualizar.
REM ============================================================
set "DEST=C:\SportGym"
set "LOG=%DEST%\INSTALAR-LOG.txt"
set "SETUP_NAME=SportGym-Acceso-Setup-1.0.0-win32.exe"
set "SETUP_URL=https://sportgymr10.com/downloads/%SETUP_NAME%"
set "GITHUB=https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/turnstile-gateway"

mkdir "%DEST%" 2>nul
echo [%date% %time%] Inicio instalador >> "%LOG%"

echo ========================================
echo  Sport Gym Acceso - Instalador
echo ========================================
echo.
echo Solo necesita ESTE archivo e internet.
echo Instala la app con logo del gym + lector de tarjeta COM3.
echo.
echo Cierre ATP-ACCESO 4.0.exe antes de continuar.
echo.
pause

REM --- 1. Lector y torniquete desde GitHub ---
echo.
echo [1/3] Descargando lector y torniquete...
mkdir "%DEST%\turnstile-gateway" 2>nul
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$base='%GITHUB%/'; $dir='%DEST%\turnstile-gateway';" ^
  "$files=@('serial_card_reader.py','turnstile_gate.py','turnstile-gate.env','turnstile-gate.env.example','sniff_gate_port.py','diagnostico_puerto.py','probar-diagnostico-com.bat','iniciar-lector-tarjeta.bat','detener-lector-tarjeta.bat','escuchar-puerta-com.bat','probar-seguro-com3.bat','probar-letras-com3.bat','poner-seguro.bat','quitar-seguro.bat','verificar-archivos.bat','SportGym-Acceso-App.bat','SportGym-Acceso-Kiosk.bat','iniciar-puesto-acceso.bat','iniciar-lector-debug.bat','iniciar-lector-115200.bat','requirements-serial.txt','LEEME-RECEPCION.txt','OPERACION-GYM.txt','ATP-ACCESO-PROTOCOLO.txt');" ^
  "$ok=0; foreach($f in $files){ try { Invoke-WebRequest -Uri ($base+'/'+$f) -OutFile (Join-Path $dir $f) -UseBasicParsing; Write-Host ('  OK '+$f); if($f -eq 'iniciar-lector-tarjeta.bat'){$ok=1} } catch { Write-Host ('  ERROR '+$f) -ForegroundColor Red } }; if(-not $ok){exit 1}"

if not exist "%DEST%\turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo.
  echo ERROR: no se pudo descargar el lector. Revise internet.
  pause
  exit /b 1
)
echo Lector OK >> "%LOG%"

REM --- 2. App .exe: local junto al bat, cache, o descarga del servidor ---
echo.
echo [2/3] Obteniendo Sport Gym Acceso.exe...
set "SETUP="

if exist "%~dp0%SETUP_NAME%" (
  set "SETUP=%~dp0%SETUP_NAME%"
  echo Usando instalador junto a este .bat
)
if not defined SETUP if exist "%DEST%\%SETUP_NAME%" (
  for %%A in ("%DEST%\%SETUP_NAME%") do if %%~zA GTR 5000000 set "SETUP=%DEST%\%SETUP_NAME%"
  if defined SETUP echo Usando instalador en cache
)

if not defined SETUP (
  echo Descargando desde sportgymr10.com ...
  echo   %SETUP_URL%
  echo Puede tardar varios minutos...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { $p='%DEST%\%SETUP_NAME%'; Invoke-WebRequest -Uri '%SETUP_URL%' -OutFile $p -UseBasicParsing; if((Get-Item $p).Length -lt 5000000){ Remove-Item $p -Force; throw 'Archivo muy pequeno - no esta publicado en el servidor' }; exit 0 } catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }"
  if exist "%DEST%\%SETUP_NAME%" set "SETUP=%DEST%\%SETUP_NAME%"
)

if not defined SETUP (
  echo.
  echo ERROR: no se pudo obtener el instalador.
  echo El administrador debe ejecutar una vez en la PC de desarrollo:
  echo   SUBIR-SETUP-AL-SERVIDOR.bat
  echo.
  echo O copie SportGym-Acceso-Setup-1.0.0-win32.exe junto a este .bat
  pause
  exit /b 1
)

REM --- 3. Instalar app ---
echo.
echo [3/3] Instalando Sport Gym Acceso...
echo Setup: !SETUP! >> "%LOG%"
start /wait "" "!SETUP!"
if errorlevel 1 echo Instalador termino con codigo de error >> "%LOG%"

REM --- Acceso directo en escritorio ---
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
  echo.
  echo  Escritorio: Sport Gym Acceso
  echo  Programa:   !TARGET!
  echo  Lector:     %DEST%\turnstile-gateway
  echo  Pantalla:   sportgymr10.com
  echo.
  echo  Abra Sport Gym Acceso del escritorio.
  echo  Para actualizar pantalla: cierre y abra la app.
  echo ========================================
  echo.
  set /p ABRIR="Abrir Sport Gym Acceso ahora? S/N: "
  if /i "!ABRIR!"=="S" start "" "!TARGET!"
) else (
  echo.
  echo AVISO: no se detecto la app instalada.
  echo Si cancelo el instalador, ejecute este .bat de nuevo
  echo y complete los pasos del Setup.exe.
)

pause
exit /b 0
