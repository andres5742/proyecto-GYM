@echo off
REM Lanzador en C:\SportGym — app desde sportgymr10.com + lector COM3
cd /d "%~dp0"
if exist "%~dp0SportGym-Acceso.vbs" (
  start "" wscript.exe "%~dp0SportGym-Acceso.vbs"
  exit /b 0
)
if exist "%~dp0turnstile-gateway\SportGym-Acceso-App.bat" (
  call "%~dp0turnstile-gateway\SportGym-Acceso-App.bat"
  exit /b 0
)
echo ERROR: Falta turnstile-gateway. Ejecute INSTALAR-SPORT-GYM-ENTRADA.bat
pause
exit /b 1
