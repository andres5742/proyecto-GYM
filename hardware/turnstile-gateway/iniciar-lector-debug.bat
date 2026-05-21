@echo off
cd /d "%~dp0"
set SERIAL_DEBUG=1
set SERIAL_BAUD=9600
call iniciar-lector-tarjeta.bat
