@echo off
setlocal EnableDelayedExpansion
title Instalar Sport Gym Acceso
REM ============================================================
REM  SOLO COPIE ESTE ARCHIVO al PC del torniquete y ejecutelo.
REM  Descarga e instala todo: app .exe + lector COM3 + acceso directo.
REM ============================================================
set "DEST=C:\SportGym"
set "LOG=%DEST%\INSTALAR-LOG.txt"
set "SETUP_NAME=SportGym-Acceso-Setup-1.0.0-win32.exe"
set "SETUP_URL=https://sportgymr10.com/downloads/%SETUP_NAME%"
set "GITHUB=https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/turnstile-gateway"
set "TLS=[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12"

mkdir "%DEST%" 2>nul
echo [%date% %time%] Inicio instalador >> "%LOG%"

echo ========================================
echo  Sport Gym Acceso - Instalador
echo ========================================
echo.
echo Solo necesita ESTE archivo e internet.
echo Instala la app con logo del gym + lector COM3.
echo.
echo Cierre ATP-ACCESO 4.0.exe antes de continuar.
echo.
pause

REM --- 1. Lector y torniquete ---
echo.
echo [1/3] Descargando lector y torniquete...
mkdir "%DEST%\turnstile-gateway" 2>nul

if exist "%~dp0turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo Copiando lector desde carpeta junto al .bat...
  xcopy /E /I /Y /Q "%~dp0turnstile-gateway\*" "%DEST%\turnstile-gateway\"
  goto :lector_ok
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "%TLS%; $ProgressPreference='SilentlyContinue'; $base='%GITHUB%'; $dir='%DEST%\turnstile-gateway';" ^
  "$files=@('serial_card_reader.py','turnstile_gate.py','turnstile-gate.env','turnstile-gate.env.example','iniciar-lector-tarjeta.bat','detener-lector-tarjeta.bat','probar-letras-com3.bat','poner-seguro.bat','quitar-seguro.bat','SportGym-Acceso-App.bat','requirements-serial.txt');" ^
  "$ok=$false; foreach($f in $files){ $url=$base+'/'+$f; $out=Join-Path $dir $f; $done=$false;" ^
  "  try { Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing; Write-Host ('  OK '+$f); $done=$true } catch { Write-Host ('  PS ERROR '+$f) -ForegroundColor Yellow }" ^
  "  if(-not $done -and (Get-Command curl.exe -ErrorAction SilentlyContinue)){ $c=Start-Process curl.exe -ArgumentList @('-fsSL','--ssl-no-revoke','-o',$out,$url) -Wait -PassThru -NoNewWindow; if($c.ExitCode -eq 0){ Write-Host ('  OK curl '+$f); $done=$true } }" ^
  "  if($f -eq 'iniciar-lector-tarjeta.bat' -and $done){$ok=$true} }; if(-not $ok){exit 1}"

if not exist "%DEST%\turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo.
  echo ERROR: no se pudo descargar el lector.
  echo Solucion USB: copie la carpeta turnstile-gateway junto a este .bat
  pause
  exit /b 1
)

:lector_ok
echo Lector OK >> "%LOG%"

REM --- 2. Instalador .exe ---
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
  set "DL=%DEST%\%SETUP_NAME%"
  where curl >nul 2>&1
  if not errorlevel 1 (
    curl.exe -fL --ssl-no-revoke -o "!DL!" "%SETUP_URL%"
    if exist "!DL!" for %%A in ("!DL!") do if %%~zA GTR 5000000 set "SETUP=!DL!"
  )
  if not defined SETUP (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "%TLS%; $ProgressPreference='SilentlyContinue'; try { $p='!DL!'; Invoke-WebRequest -Uri '%SETUP_URL%' -OutFile $p -UseBasicParsing; if((Get-Item $p).Length -lt 5000000){ Remove-Item $p -Force; throw 'Archivo invalido en servidor' }; exit 0 } catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }"
    if exist "!DL!" for %%A in ("!DL!") do if %%~zA GTR 5000000 set "SETUP=!DL!"
  )
)

if not defined SETUP (
  echo.
  echo ERROR: no se pudo descargar el instalador.
  echo.
  echo OPCION A - USB: copie junto a este .bat:
  echo   %SETUP_NAME%
  echo y vuelva a ejecutar.
  echo.
  echo OPCION B - En PC de desarrollo ejecute:
  echo   SUBIR-SETUP-AL-SERVIDOR.bat
  pause
  exit /b 1
)

REM --- 3. Instalar ---
echo.
echo [3/3] Instalando Sport Gym Acceso...
echo Setup: !SETUP! >> "%LOG%"
start /wait "" "!SETUP!"

set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "TARGET="
set "WORKDIR="
if exist "%DEST%\Sport Gym Acceso.exe" (
  set "TARGET=%DEST%\Sport Gym Acceso.exe" & set "WORKDIR=%DEST%"
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
  echo  Programa:   !TARGET!
  echo  Lector:     %DEST%\turnstile-gateway
  echo ========================================
  set /p ABRIR="Abrir Sport Gym Acceso ahora? S/N: "
  if /i "!ABRIR!"=="S" start "" "!TARGET!"
) else (
  echo AVISO: complete la instalacion del Setup si la cancelo.
)

pause
exit /b 0
