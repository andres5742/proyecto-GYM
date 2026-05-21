@echo off
title Sport Gym - Descargar archivos del lector
cd /d "%~dp0"

echo Descargando archivos del lector desde GitHub...
echo Carpeta: %CD%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$base='https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/turnstile-gateway/';" ^
  "$dir='%CD%';" ^
  "$files=@('serial_card_reader.py','iniciar-lector-tarjeta.bat','detener-lector-tarjeta.bat','requirements-serial.txt');" ^
  "foreach($f in $files){$out=Join-Path $dir $f; try { Invoke-WebRequest -Uri ($base+$f) -OutFile $out -UseBasicParsing; Write-Host ('OK: '+$f) } catch { Write-Host ('ERROR: '+$f+' - '+$_.Exception.Message) -ForegroundColor Red } }"

echo.
if exist "serial_card_reader.py" (
  echo Listo. Ahora ejecute: iniciar-lector-tarjeta.bat
) else (
  echo Fallo la descarga. Revise internet o copie la carpeta desde USB.
)
echo.
pause
