@echo off
title Sport Gym - Probar DESBLOQUEO tras bloqueo con l
cd /d "%~dp0"
pip install pyserial -q

echo 1) Enviando L para BLOQUEAR (confirmado en su gym)
python -c "import serial,time; s=serial.Serial('COM3',19200,timeout=1); s.write(b'l'); s.flush(); print('Bloqueo l enviado'); time.sleep(2); s.close()"

echo.
echo 2) Pruebe cada letra para DESBLOQUEAR (empuje el torniquete tras cada una):
echo    a  d  m  n  g  h  (h/i/l bloquean, no desbloquean)
echo.
pause

for %%L in (a d m n g) do (
  echo --- Desbloqueo prueba: %%L ---
  python -c "import serial,time; s=serial.Serial('COM3',19200,timeout=1); s.write(b'%%L'); s.flush(); print('Enviado %%L - empuje ahora'); time.sleep(6); s.write(b'l'); s.flush(); print('Re-bloqueo l'); s.close()"
  pause
)

echo Anote cual letra permitio PASAR. Ponga esa en turnstile-gate.env como TURNSTILE_UNLOCK_CHAR=
pause
