@echo off
cd /d "%~dp0"
echo Poniendo seguro en torniquete (d @ 19200)...
python turnstile_gate.py lock
echo Si el lector esta abierto, la orden se encola en iniciar-lector-tarjeta.bat
pause
