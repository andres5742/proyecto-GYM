@echo off
setlocal EnableDelayedExpansion
title Sport Gym - Instalador oficial torniquete
REM ============================================================
REM  INSTALADOR OFICIAL — PC del torniquete (instalar y actualizar).
REM  Copie SOLO este archivo al PC y ejecutelo con internet.
REM  Descarga GitHub + Setup .exe + acceso directo logo gym.
REM
REM  GitHub:
REM  https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
REM ============================================================
set "DEST=C:\SportGym"
set "GW=%DEST%\turnstile-gateway"
set "LOG=%DEST%\ACTUALIZAR-LOG.txt"
set "SETUP_NAME=SportGym-Acceso-Setup-1.0.0-win32.exe"
set "SETUP_URL=https://sportgymr10.com/downloads/%SETUP_NAME%"
set "GIT_GW=https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/turnstile-gateway"
set "GIT_HW=https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware"
set "TLS=[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12"

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

REM --- Cerrar app anterior ---
taskkill /IM "Sport Gym Acceso.exe" /F >nul 2>&1
timeout /t 2 /nobreak >nul

REM --- 1. GitHub: lector y torniquete ---
echo.
echo [1/4] Descargando lector y torniquete desde GitHub...
call :DownloadGit "%GIT_GW%" "%GW%" "serial_card_reader.py"
call :DownloadGit "%GIT_GW%" "%GW%" "turnstile_gate.py"
call :DownloadGit "%GIT_GW%" "%GW%" "turnstile-gate.env"
call :DownloadGit "%GIT_GW%" "%GW%" "turnstile-gate.env.example"
call :DownloadGit "%GIT_GW%" "%GW%" "iniciar-lector-tarjeta.bat"
call :DownloadGit "%GIT_GW%" "%GW%" "detener-lector-tarjeta.bat"
call :DownloadGit "%GIT_GW%" "%GW%" "probar-letras-com3.bat"
call :DownloadGit "%GIT_GW%" "%GW%" "probar-seguro-com3.bat"
call :DownloadGit "%GIT_GW%" "%GW%" "poner-seguro.bat"
call :DownloadGit "%GIT_GW%" "%GW%" "quitar-seguro.bat"
call :DownloadGit "%GIT_GW%" "%GW%" "requirements-serial.txt"
call :DownloadGit "%GIT_GW%" "%GW%" "OPERACION-GYM.txt"
call :DownloadGit "%GIT_GW%" "%GW%" "ATP-ACCESO-PROTOCOLO.txt"

if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  if exist "%~dp0turnstile-gateway\iniciar-lector-tarjeta.bat" (
    echo Copiando lector desde USB junto al .bat...
    xcopy /E /I /Y /Q "%~dp0turnstile-gateway\*" "%GW%\"
  )
)
if not exist "%GW%\iniciar-lector-tarjeta.bat" (
  echo ERROR: no se descargo el lector. Revise internet o copie turnstile-gateway por USB.
  pause
  exit /b 1
)
echo Lector OK >> "%LOG%"

REM --- 2. Icono y lanzadores ---
echo.
echo [2/4] Descargando icono y lanzadores...
call :DownloadGit "%GIT_HW%" "%DEST%" "LEEME-TORNIQUETE.txt"
call :DownloadGit "%GIT_HW%" "%DEST%" "SportGym.ico"
call :DownloadGit "%GIT_HW%" "%DEST%" "INICIAR-ACCESO-COMPLETO.bat"
if exist "%~dp0INICIAR-ACCESO-COMPLETO.bat" copy /Y "%~dp0INICIAR-ACCESO-COMPLETO.bat" "%DEST%\INICIAR-ACCESO-COMPLETO.bat" >nul 2>&1
if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico" >nul 2>&1

REM --- 3. Setup .exe ---
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
      "%TLS%; $ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -Uri '%SETUP_URL%' -OutFile '!DL!' -UseBasicParsing; if((Get-Item '!DL!').Length -lt 5000000){ throw 'archivo invalido' }; exit 0 } catch { exit 1 }"
    if exist "!DL!" for %%A in ("!DL!") do if %%~zA GTR 5000000 set "SETUP=!DL!"
  )
)
if not defined SETUP (
  echo AVISO: no se descargo el Setup. Se actualiza lector + acceso directo solamente.
  echo Copie %SETUP_NAME% junto a este .bat y vuelva a ejecutar para reinstalar la app.
  goto :shortcut
)

echo Instalando / reinstalando...
echo Setup: !SETUP! >> "%LOG%"
start /wait "" "!SETUP!"

REM Copiar icono desde app instalada si falta
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

:shortcut
REM --- 4. Acceso directo con logo del gym ---
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
echo  Lector: %GW%
echo ========================================
echo.
echo Si el icono no cambia de inmediato: clic derecho en el acceso directo
echo - Propiedades - Cambiar icono - elija %DEST%\SportGym.ico
echo.
set /p ABRIR="Abrir Sport Gym Acceso ahora? S/N: "
if /i "!ABRIR!"=="S" call "%DEST%\INICIAR-ACCESO-COMPLETO.bat"
pause
exit /b 0

:DownloadGit
set "BASE=%~1"
set "DIR=%~2"
set "FILE=%~3"
set "URL=!BASE!/!FILE!"
set "OUT=!DIR!\!FILE!"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "%TLS%; $ProgressPreference='SilentlyContinue'; $url='!URL!'; $out='!OUT!'; $done=$false;" ^
  "try { Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing; Write-Host ('  OK '+$url); $done=$true } catch { }" ^
  "if(-not $done -and (Get-Command curl.exe -ErrorAction SilentlyContinue)){ $c=Start-Process curl.exe -ArgumentList @('-fsSL','--ssl-no-revoke','-o',$out,$url) -Wait -PassThru -NoNewWindow; if($c.ExitCode -eq 0){ Write-Host ('  OK curl '+$url); $done=$true } }" ^
  "if(-not $done){ Write-Host ('  FALLO '+$url) -ForegroundColor Yellow }"
exit /b 0

:EnsureGymIcon
if exist "%DEST%\SportGym.ico" exit /b 0
if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico" >nul & exit /b 0
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" (
  copy /Y "%LOCALAPPDATA%\Programs\Sport Gym Acceso\resources\SportGym.ico" "%DEST%\SportGym.ico" >nul
)
exit /b 0

:CreateDesktopShortcut
set "LAUNCHER=%DEST%\INICIAR-ACCESO-COMPLETO.bat"
set "LINK=%USERPROFILE%\Desktop\Sport Gym Acceso.lnk"
set "ICON=%DEST%\SportGym.ico"

if not exist "%LAUNCHER%" (
  echo ERROR: falta %LAUNCHER%
  exit /b 1
)

del "%LINK%" 2>nul

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$launcher='%LAUNCHER%'; $link='%LINK%'; $icon='%ICON%'; $dest='%DEST%';" ^
  "$w=New-Object -ComObject WScript.Shell; $s=$w.CreateShortcut($link);" ^
  "$s.TargetPath=$launcher; $s.WorkingDirectory=$dest;" ^
  "$s.Description='Sport Gym Acceso - lector COM3 + pantalla';" ^
  "if (Test-Path $icon) { $s.IconLocation = $icon + ',0' } else {" ^
  "  Write-Host 'AVISO: falta SportGym.ico - icono generico' -ForegroundColor Yellow };" ^
  "$s.Save(); Write-Host ('Acceso directo: '+$link); if (Test-Path $icon) { Write-Host ('Icono gym: '+$icon) }"
exit /b 0
