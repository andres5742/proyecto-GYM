@echo off
cd /d "%~dp0"
set SERIAL_BAUD=115200
set SERIAL_DEBUG=1
call iniciar-lector-tarjeta.bat
