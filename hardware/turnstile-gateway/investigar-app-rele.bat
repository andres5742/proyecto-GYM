@echo off
title Sport Gym - Investigar app que abre el rele
cd /d "%~dp0"

set LOG=%~dp0investigacion-rele-%date:~-4,4%%date:~3,2%%date:~0,2%-%time:~0,2%%time:~3,2%%time:~6,2%.txt
set LOG=%LOG: =0%

echo INVESTIGACION APP TORNiquete > "%LOG%"
echo Fecha: %date% %time% >> "%LOG%"
echo PC: %COMPUTERNAME% >> "%LOG%"
echo Usuario: %USERNAME% >> "%LOG%"
echo. >> "%LOG%"

echo === 1. Procesos .exe (nombre y ruta) === >> "%LOG%"
wmic process where "name='exe'" get Name,ProcessId,ExecutablePath 2>nul >> "%LOG%"
tasklist /FI "IMAGENAME eq *.exe" /V 2>nul >> "%LOG%"
echo. >> "%LOG%"

echo === 2. Puertos COM === >> "%LOG%"
wmic path Win32_SerialPort get DeviceID,Name,Description 2>nul >> "%LOG%"
echo. >> "%LOG%"

echo === 3. Conexiones de red ANTES (copie esto) === >> "%LOG%"
netstat -ano | findstr ESTABLISHED >> "%LOG%"
netstat -ano | findstr LISTENING >> "%LOG%"
echo. >> "%LOG%"

echo.
echo ============================================================
echo  INSTRUCCIONES (lea en pantalla, no solo el log)
echo ============================================================
echo.
echo 1. Cierre iniciar-lector-tarjeta.bat y Sport Gym.
echo 2. Anote el nombre del .exe que SI abre el rele (ej. ZKAccess.exe).
echo 3. Abra SOLO esa app.
echo 4. Pulse una tecla AQUI para guardar "ANTES" en el log.
pause >> "%LOG%"
echo --- ANTES de abrir puerta --- >> "%LOG%"
netstat -ano >> "%LOG%"

echo.
echo 5. En la otra app: abra el torniquete (tarjeta o boton).
echo 6. Vuelva aqui y pulse una tecla para guardar "DURANTE".
pause >> "%LOG%"
echo --- DURANTE despues de abrir puerta --- >> "%LOG%"
netstat -ano >> "%LOG%"

echo.
echo 7. Si tiene Process Monitor (Procmon) de Microsoft:
echo    Filtro: Process Name is SU_EXE.exe
echo    Ver: WriteFile, DeviceIoControl, TCP Connect
echo.
echo 8. En la app vieja busque menu:
echo    Dispositivo / Comunicacion / IP / Puerto / Door / Relay
echo    y anote IP (ej. 192.168.101.20) y puerto (ej. 4370).
echo.
echo Log guardado en:
echo   %LOG%
echo.
echo Envie ese archivo .txt o captura de IP/puerto del dispositivo.
echo.
pause
