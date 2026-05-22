@echo off
title Sport Gym - Escuchar puerto torniquete
cd /d "%~dp0"

where python >nul 2>&1
if errorlevel 1 (
  echo Instale Python con "Add to PATH"
  pause
  exit /b 1
)

pip install pyserial -q

REM En su gym el enlace cafe-^>PC es COM3 (no COM4)
set SNIFF_PORT=COM3
set SNIFF_BAUD=9600
set SNIFF_SECONDS=90

echo.
echo Puerto a escuchar: %SNIFF_PORT%  (edite SNIFF_PORT si hace falta)
echo.
echo INSTRUCCIONES:
echo   1. Cierre iniciar-lector-tarjeta.bat y ZKAccess.
echo   2. Ejecute detener-lector-tarjeta.bat
echo   3. Pulse una tecla aqui y ESPERE a que empiece el cronometro.
echo   4. ENTONCES abra ZKAccess y abra/cierre el torniquete varias veces.
echo   5. Tambien pase tarjeta en el lector de pared.
echo   6. Anote hex que diga RECIBIDO.
echo.
echo Si no sale NADA en COM3: ZKAccess abre por la placa cafe sin mandar datos al PC.
echo   En ese caso use: probar-seguro-com3.bat
echo.
pause

python sniff_gate_port.py
echo.
pause
