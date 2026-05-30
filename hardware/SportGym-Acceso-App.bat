@echo off
REM Lanzador en C:\SportGym — solo app de escritorio (sin navegador).
cd /d "%~dp0"
if exist "%~dp0ABRIR-SPORT-GYM-ACCESO.vbs" (
  start "" wscript.exe "%~dp0ABRIR-SPORT-GYM-ACCESO.vbs"
  exit /b 0
)
if exist "%~dp0SportGym-Acceso.vbs" (
  start "" wscript.exe "%~dp0SportGym-Acceso.vbs"
  exit /b 0
)
if exist "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe"
  exit /b 0
)
if exist "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe"
  exit /b 0
)
if exist "%ProgramFiles(x86)%\Sport Gym Acceso\Sport Gym Acceso.exe" (
  start "" "%ProgramFiles(x86)%\Sport Gym Acceso\Sport Gym Acceso.exe"
  exit /b 0
)
echo ERROR: No se encontro Sport Gym Acceso.exe
echo Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat para instalar la app.
pause
exit /b 1
