@echo off
chcp 65001 >nul
title Cerrar Sport Gym Acceso
echo Cerrando aplicacion y lector de tarjeta...
taskkill /IM "Sport Gym Acceso.exe" /F 2>nul
taskkill /IM "python.exe" /FI "WINDOWTITLE eq Lector*" /F 2>nul
cd /d "C:\SportGym\turnstile-gateway" 2>nul
if exist detener-lector-tarjeta.bat call detener-lector-tarjeta.bat
echo Listo.
timeout /t 2 >nul
