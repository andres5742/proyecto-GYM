@echo off
title Sport Gym - Secuencia a luego h
cd /d "%~dp0"
pip install pyserial -q

set TURNSTILE_GATE_MODE=serial
set TURNSTILE_GATE_PROTOCOL=atp-acceso
set TURNSTILE_GATE_PORT=COM3
set TURNSTILE_GATE_BAUD=19200

echo Prueba: l=bloquear, a=liberar, l=bloquear de nuevo
echo Cierre ATP-ACCESO antes. Empuje al mandar a.
pause

python -c "import serial,time; s=serial.Serial('COM3',19200,timeout=1); s.write(b'l'); s.flush(); time.sleep(2); s.write(b'a'); s.flush(); print('l bloqueo, a libera — EMPUJE 8 s'); time.sleep(8); s.write(b'l'); s.flush(); print('l bloqueo otra vez'); s.close()"

pause
