@echo off
setlocal EnableDelayedExpansion
title Sport Gym - Instalador oficial torniquete
REM ============================================================
REM  INSTALADOR OFICIAL — PC del torniquete (instalar y actualizar).
REM  Copie SOLO este archivo al PC y ejecutelo con internet.
REM
REM  GitHub:
REM  https://raw.githubusercontent.com/andres5742/proyecto-GYM/aplicacion_torniquete/hardware/ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
REM ============================================================
goto :Main

:DownloadGit
set "BASE=%~1"
set "DIR=%~2"
set "FILE=%~3"
set "URL=!BASE!/!FILE!"
set "OUT=!DIR!\!FILE!"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue';" ^
  "$url='!URL!'; $out='!OUT!'; $done=$false;" ^
  "try { Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing; Write-Host ('  OK '+$url); $done=$true } catch { }" ^
  "if(-not $done -and (Get-Command curl.exe -ErrorAction SilentlyContinue)){ $c=Start-Process curl.exe -ArgumentList @('-fsSL','--ssl-no-revoke','-o',$out,$url) -Wait -PassThru -NoNewWindow; if($c.ExitCode -eq 0){ Write-Host ('  OK curl '+$url); $done=$true } }" ^
  "if(-not $done){ Write-Host ('  FALLO '+$url) -ForegroundColor Yellow; exit 1 }"
exit /b %ERRORLEVEL%

:EnsureGymIcon
if exist "%DEST%\SportGym.ico" exit /b 0
if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico" >nul & exit /b 0
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" (
  copy /Y "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" "%DEST%\SportGym.ico" >nul
)
exit /b 0

:CreateDesktopShortcut
set "LAUNCHER=%DEST%\ABRIR-SPORT-GYM-ACCESO.vbs"
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "ICON=%DEST%\SportGym.ico"
if not exist "%LAUNCHER%" set "LAUNCHER=%DEST%\INICIAR-ACCESO-COMPLETO.bat"
if not exist "%LAUNCHER%" (
  echo ERROR: falta lanzador en %DEST%
  exit /b 1
)
del "%LINK%" 2>nul
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$launcher='%LAUNCHER%'; $link='%LINK%'; $icon='%ICON%'; $dest='%DEST%';" ^
  "$w=New-Object -ComObject WScript.Shell; $s=$w.CreateShortcut($link);" ^
  "if ($launcher -like '*.vbs') { $s.TargetPath = $env:Windir + '\System32\wscript.exe'; $s.Arguments = '\"' + $launcher + '\"'; } else { $s.TargetPath = $launcher; }" ^
  "$s.WorkingDirectory=$dest;" ^
  "$s.Description='Sport Gym Acceso - lector COM3 + pantalla';" ^
  "if (Test-Path $icon) { $s.IconLocation = $icon + ',0' } else {" ^
  "  Write-Host 'AVISO: falta SportGym.ico - icono generico' -ForegroundColor Yellow };" ^
  "$s.Save(); Write-Host ('Acceso directo: '+$link); if (Test-Path $icon) { Write-Host ('Icono gym: '+$icon) }"
exit /b 0

:Main
set "DEST=C:\SportGym"
set "GW=%DEST%\turnstile-gateway"
set "LOG=%DEST%\ACTUALIZAR-LOG.txt"
set "SETUP_NAME=SportGym-Acceso-Setup-1.0.0-win32.exe"
set "SETUP_URL=https://sportgymr10.com/downloads/%SETUP_NAME%"
set "GIT_BRANCH=aplicacion_torniquete"
set "GIT_GW=https://raw.githubusercontent.com/andres5742/proyecto-GYM/%GIT_BRANCH%/hardware/turnstile-gateway"
set "GIT_HW=https://raw.githubusercontent.com/andres5742/proyecto-GYM/%GIT_BRANCH%/hardware"

mkdir "%DEST%" 2>nul
mkdir "%GW%" 2>nul
echo [%date% %time%] Inicio actualizacion >> "%LOG%"

echo ========================================
echo  Sport Gym Acceso - Actualizar todo
echo ========================================
echo.
echo 1. Descarga lector + torniquete (GitHub)
echo 2. Descarga icono logo del gym
echo 3. Instala / reinstala Sport Gym Acceso.exe
echo 4. Acceso directo en escritorio (logo gym)
echo.
echo Cierre ATP-ACCESO 4.0.exe y Sport Gym Acceso antes.
echo.
pause

taskkill /IM "Sport Gym Acceso.exe" /F >nul 2>&1
timeout /t 2 /nobreak >nul

echo.
echo [1/4] Descargando lector y torniquete desde GitHub...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue';" ^
  "$base='%GIT_GW%/'; $dir='%GW%';" ^
  "$files=@('serial_card_reader.py','turnstile_gate.py','zkt_card_event.py','simulate_scan.py','diagnostico_puerto.py','sniff_gate_port.py','probar_letras.py','turnstile-gate.env','turnstile-gate.env.example','iniciar-lector-tarjeta.bat','detener-lector-tarjeta.bat','preparar-com3.bat','probar-letras-com3.bat','probar-seguro-com3.bat','probar-diagnostico-com.bat','escuchar-puerta-com.bat','poner-seguro.bat','quitar-seguro.bat','verificar-archivos.bat','iniciar-lector-debug.bat','iniciar-lector-115200.bat','instalar-inicio-automatico.bat','quitar-inicio-automatico.bat','bloquear-torniquete-arranque.bat','SportGym-Acceso-App.bat','SportGym-Acceso-Kiosk.bat','iniciar-puesto-acceso.bat','requirements-serial.txt','OPERACION-GYM.txt','ATP-ACCESO-PROTOCOLO.txt','LEEME-RECEPCION.txt','ARQUITECTURA-PLACAS.txt','SITUACION-COM3.txt');" ^
  "$fail=0; foreach($f in $files){ $out=Join-Path $dir $f; $ok=$false;" ^
  "  try { Invoke-WebRequest -Uri ($base+$f) -OutFile $out -UseBasicParsing; Write-Host ('  OK '+$f); $ok=$true } catch { }" ^
  "  if(-not $ok -and (Get-Command curl.exe -ErrorAction SilentlyContinue)){ $c=Start-Process curl.exe -ArgumentList @('-fsSL','--ssl-no-revoke','-o',$out,($base+$f)) -Wait -PassThru -NoNewWindow; if($c.ExitCode -eq 0){ Write-Host ('  OK curl '+$f); $ok=$true } }" ^
  "  if(-not $ok){ Write-Host ('  FALLO '+$f) -ForegroundColor Yellow; $fail++ } }; exit $fail"

if exist "%~dp0turnstile-gateway\iniciar-lector-tarjeta.bat" (
  echo Copiando lector desde USB junto al .bat...
  xcopy /E /I /Y /Q "%~dp0turnstile-gateway\*" "%GW%\"
)

if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  echo.
  echo ERROR: no se descargo el lector en %GW%
  echo Revise internet, antivirus o copie la carpeta turnstile-gateway por USB.
  echo Luego vuelva a ejecutar este .bat.
  pause
  exit /b 1
)
echo Lector OK >> "%LOG%"

echo.
echo [2/4] Descargando icono y lanzadores...
call :DownloadGit "%GIT_HW%" "%DEST%" "LEEME-TORNIQUETE.txt"
call :DownloadGit "%GIT_HW%" "%DEST%" "SportGym.ico"
call :DownloadGit "%GIT_HW%" "%DEST%" "INICIAR-ACCESO-COMPLETO.bat"
call :DownloadGit "%GIT_HW%" "%DEST%" "ABRIR-SPORT-GYM-ACCESO.vbs"
call :DownloadGit "%GIT_HW%" "%DEST%" "iniciar-arranque-windows.bat"
if exist "%~dp0INICIAR-ACCESO-COMPLETO.bat" copy /Y "%~dp0INICIAR-ACCESO-COMPLETO.bat" "%DEST%\INICIAR-ACCESO-COMPLETO.bat" >nul 2>&1
if exist "%~dp0ABRIR-SPORT-GYM-ACCESO.vbs" copy /Y "%~dp0ABRIR-SPORT-GYM-ACCESO.vbs" "%DEST%\ABRIR-SPORT-GYM-ACCESO.vbs" >nul 2>&1
if exist "%~dp0iniciar-arranque-windows.bat" copy /Y "%~dp0iniciar-arranque-windows.bat" "%DEST%\iniciar-arranque-windows.bat" >nul 2>&1
if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico" >nul 2>&1

echo.
echo [3/4] Descargando Sport Gym Acceso (instalador)...
set "SETUP="
if exist "%~dp0%SETUP_NAME%" set "SETUP=%~dp0%SETUP_NAME%"
if not defined SETUP if exist "%DEST%\%SETUP_NAME%" (
  for %%A in ("%DEST%\%SETUP_NAME%") do if %%~zA GTR 5000000 set "SETUP=%DEST%\%SETUP_NAME%"
)
if not defined SETUP (
  set "DL=%DEST%\%SETUP_NAME%"
  echo   %SETUP_URL%
  where curl >nul 2>&1
  if not errorlevel 1 (
    curl.exe -fL --ssl-no-revoke -o "!DL!" "%SETUP_URL%"
    if exist "!DL!" for %%A in ("!DL!") do if %%~zA GTR 5000000 set "SETUP=!DL!"
  )
  if not defined SETUP (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -Uri '%SETUP_URL%' -OutFile '!DL!' -UseBasicParsing; if((Get-Item '!DL!').Length -lt 5000000){ throw 'archivo invalido' }; exit 0 } catch { exit 1 }"
    if exist "!DL!" for %%A in ("!DL!") do if %%~zA GTR 5000000 set "SETUP=!DL!"
  )
)
if not defined SETUP (
  echo AVISO: no se descargo el Setup. Se actualiza lector + acceso directo solamente.
  echo Copie %SETUP_NAME% junto a este .bat y vuelva a ejecutar para reinstalar la app.
  goto :Shortcut
)

echo Instalando / reinstalando...
echo Setup: !SETUP! >> "%LOG%"
start /wait "" "!SETUP!"

if not exist "%DEST%\SportGym.ico" (
  if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" (
    copy /Y "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" "%DEST%\SportGym.ico" >nul
  )
)
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\turnstile-gateway" (
  xcopy /E /I /Y /Q "%GW%\*" "%LOCALAPPDATA%\Programs\Sport Gym Acceso\turnstile-gateway\" >nul 2>&1
) else (
  mkdir "%LOCALAPPDATA%\Programs\Sport Gym Acceso\turnstile-gateway" 2>nul
  xcopy /E /I /Y /Q "%GW%\*" "%LOCALAPPDATA%\Programs\Sport Gym Acceso\turnstile-gateway\" >nul 2>&1
)

:Shortcut
if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  echo.
  echo ERROR: falta %GW%\iniciar-lector-tarjeta.bat
  echo No se puede continuar sin el lector. Revise internet o copie turnstile-gateway por USB.
  pause
  exit /b 1
)

echo.
echo [4/4] Creando acceso directo (logo Sport Gym)...
call :EnsureGymIcon
call :CreateDesktopShortcut

echo.
echo ========================================
echo  LISTO
echo  Escritorio: Sport Gym Acceso
echo  Icono: %DEST%\SportGym.ico
echo  Lanzador: %DEST%\INICIAR-ACCESO-COMPLETO.bat
echo  Arranque Windows: %DEST%\iniciar-arranque-windows.bat
echo  Lector: %GW%
echo ========================================
echo.
echo Si el icono no cambia de inmediato: clic derecho en el acceso directo
echo - Propiedades - Cambiar icono - elija %DEST%\SportGym.ico
echo.
set /p AUTO="Instalar inicio automatico al encender Windows? S/N: "
if /i "!AUTO!"=="S" if exist "%GW%\instalar-inicio-automatico.bat" call "%GW%\instalar-inicio-automatico.bat"
echo.
set /p ABRIR="Abrir Sport Gym Acceso ahora? S/N: "
if /i "!ABRIR!"=="S" call "%DEST%\INICIAR-ACCESO-COMPLETO.bat"
pause
exit /b 0
