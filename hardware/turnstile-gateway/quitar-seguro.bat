@echo off
cd /d "%~dp0"
echo Cierra ATP-ACCESO 4.0.exe e iniciar-lector-tarjeta.bat antes de probar.
python turnstile_gate.py unlock
echo Espere 8 s: el lector activo repone seguro solo. Si no hay lector, use poner-seguro.bat.
pause
