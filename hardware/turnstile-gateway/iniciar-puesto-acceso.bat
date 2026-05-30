@echo off
title Sport Gym - Puesto acceso
cd /d "%~dp0"

echo Iniciando lector de tarjeta + app de escritorio...
echo.

if exist "%~dp0preparar-com3.bat" call "%~dp0preparar-com3.bat"

REM 1) Lector en ventana aparte (debe quedar abierta)
start "Sport Gym - Lector tarjeta COM3" cmd /k call "%~dp0iniciar-lector-tarjeta.bat"

REM 2) Esperar a que el lector abra COM3
timeout /t 5 /nobreak >nul

REM 3) Abrir app de escritorio (sin navegador)
if exist "%~dp0SportGym-Acceso-App.bat" (
  call "%~dp0SportGym-Acceso-App.bat"
) else (
  echo ERROR: No se encontro SportGym-Acceso-App.bat.
  echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat para reinstalar.
)

echo.
echo Listo:
echo   - Ventana "Lector tarjeta": dejela abierta (lee tarjetas).
echo   - App de escritorio: pantalla de acceso (bienvenida automatica).
echo.
echo La puerta fisica se abre con TURNSTILE_WEBHOOK en el servidor (ver LEEME-RECEPCION.txt).
echo.
pause
