@echo off
title Sport Gym - Verificar archivos
cd /d "%~dp0"
echo Carpeta: %CD%
echo.
set OK=1
for %%f in (
  serial_card_reader.py
  turnstile_gate.py
  sniff_gate_port.py
  iniciar-lector-tarjeta.bat
  detener-lector-tarjeta.bat
  turnstile-gate.env
  turnstile-gate.env.example
) do (
  if exist "%%f" (echo [OK] %%f) else (echo [FALTA] %%f & set OK=0)
)
echo.
if "%OK%"=="1" (
  echo Todo listo para lector y pruebas de seguro.
) else (
  echo.
  echo Copie TODA la carpeta turnstile-gateway desde USB
  echo o ejecute descargar-lector-recepcion.bat con internet.
  echo Si descargo de GitHub y sigue faltando, suba el proyecto a Git primero.
)
echo.
pause
