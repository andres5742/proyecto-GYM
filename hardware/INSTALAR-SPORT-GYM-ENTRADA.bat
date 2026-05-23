@echo off
REM Instalador oficial (alias). Siempre use ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat
echo Sport Gym — redirigiendo al instalador oficial...
cd /d "%~dp0"
call "%~dp0ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat"
exit /b %ERRORLEVEL%
