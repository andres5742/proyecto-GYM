@echo off
title Sport Gym - Instalar inicio automatico
cd /d "%~dp0"

set "DEST=C:\SportGym"
set "LAUNCHER=%DEST%\iniciar-arranque-windows.bat"
set STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set LINK=%STARTUP%\SportGym-Puesto-Acceso.bat

if not exist "%LAUNCHER%" if exist "%~dp0..\iniciar-arranque-windows.bat" (
  copy /Y "%~dp0..\iniciar-arranque-windows.bat" "%LAUNCHER%" >nul
)

echo Instalando arranque automatico Sport Gym...
echo.
echo Destino Startup:
echo %STARTUP%
echo.

(
  echo @echo off
  echo REM Sport Gym - no editar; use instalar-inicio-automatico.bat para reinstalar
  echo if exist "C:\SportGym\iniciar-arranque-windows.bat" ^(
  echo   call "C:\SportGym\iniciar-arranque-windows.bat"
  echo   exit /b 0
  echo ^)
  echo if exist "C:\SportGym\turnstile-gateway\SportGym-Acceso-App.bat" ^(
  echo   call "C:\SportGym\turnstile-gateway\SportGym-Acceso-App.bat"
  echo   exit /b 0
  echo ^)
  echo echo Falta C:\SportGym. Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
  echo timeout /t 30
) > "%LINK%"

if not exist "%LINK%" (
  echo ERROR: no se pudo crear %LINK%
  pause
  exit /b 1
)

if not exist "%LAUNCHER%" (
  echo AVISO: Copie iniciar-arranque-windows.bat a C:\SportGym\
  echo         Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
) else (
  echo OK: lanzador principal en %LAUNCHER%
)

echo.
echo Al iniciar sesion en Windows:
echo   1. Cierra ATP-ACCESO (libera COM3)
echo   2. Abre Sport Gym Acceso + lector COM3
echo   3. Bloquea el torniquete (hi)
echo.
echo Para quitar: quitar-inicio-automatico.bat
echo.
pause
