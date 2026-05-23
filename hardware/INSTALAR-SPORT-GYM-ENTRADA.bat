@echo off
REM Instalador completo — redirige al script de actualizacion desde GitHub.
cd /d "%~dp0"
call "%~dp0ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat"
exit /b %ERRORLEVEL%
