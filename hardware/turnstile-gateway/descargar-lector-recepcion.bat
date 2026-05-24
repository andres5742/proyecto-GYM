@echo off
title Sport Gym - Descargar archivos del lector
cd /d "%~dp0"

echo Para INSTALAR o ACTUALIZAR todo (app .exe + lector + acceso directo):
echo   Ejecute en el PC del torniquete: ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
echo   Ver hardware\LEEME-TORNIQUETE.txt
echo.
echo Este script solo descarga archivos del lector en ESTA carpeta.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$base='https://raw.githubusercontent.com/andres5742/proyecto-GYM/master/hardware/turnstile-gateway/';" ^
  "$dir='%CD%';" ^
  "$files=@('serial_card_reader.py','turnstile_gate.py','turnstile-gate.env','turnstile-gate.env.example','sniff_gate_port.py','diagnostico_puerto.py','probar-diagnostico-com.bat','iniciar-lector-tarjeta.bat','detener-lector-tarjeta.bat','preparar-com3.bat','escuchar-puerta-com.bat','probar-seguro-com3.bat','poner-seguro.bat','quitar-seguro.bat','verificar-archivos.bat','OPERACION-GYM.txt','ATP-ACCESO-PROTOCOLO.txt','SportGym-Acceso-App.bat','SportGym-Acceso-Kiosk.bat','iniciar-puesto-acceso.bat','instalar-inicio-automatico.bat','quitar-inicio-automatico.bat','iniciar-lector-debug.bat','iniciar-lector-115200.bat','requirements-serial.txt','LEEME-RECEPCION.txt');" ^
  "foreach($f in $files){$out=Join-Path $dir $f; try { Invoke-WebRequest -Uri ($base+$f) -OutFile $out -UseBasicParsing; Write-Host ('OK: '+$f) } catch { Write-Host ('ERROR: '+$f+' - '+$_.Exception.Message) -ForegroundColor Red } }"

echo.
if exist "serial_card_reader.py" (
  echo Listo. Ahora ejecute: iniciar-lector-tarjeta.bat
) else (
  echo Fallo la descarga. Revise internet o copie la carpeta desde USB.
)
echo.
pause
