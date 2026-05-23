@echo off
cd /d "%~dp0"
echo Cierra ATP-ACCESO 4.0.exe e iniciar-lector-tarjeta.bat antes de probar.
python turnstile_gate.py unlock --wait
echo Seguro repuesto automaticamente.
pause
