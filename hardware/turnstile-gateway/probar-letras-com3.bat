@echo off
title Sport Gym - Probar letras COM3
cd /d "%~dp0"
pip install pyserial -q
echo Cierre ATP-ACCESO e iniciar-lector-tarjeta.bat
pause
set TURNSTILE_GATE_PORT=COM3
python probar_letras.py
pause
