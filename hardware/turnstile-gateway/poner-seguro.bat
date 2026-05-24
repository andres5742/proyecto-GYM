@echo off
cd /d "%~dp0"
echo Poniendo seguro en torniquete (h + i @ 19200)...
python turnstile_gate.py lock
echo Listo. Revise gate.log si no se movio el torniquete.
pause
