@echo off
chcp 65001 >nul
set "DEST=C:\SportGym"
if not exist "%DEST%\Sport Gym Acceso.exe" (
  echo No esta instalado en %DEST%
  pause
  exit /b 1
)
if exist "%~dp0SportGym.ico" copy /Y "%~dp0SportGym.ico" "%DEST%\SportGym.ico"
if exist "%DEST%\resources\SportGym.ico" copy /Y "%DEST%\resources\SportGym.ico" "%DEST%\SportGym.ico"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$d=[Environment]::GetFolderPath('Desktop');" ^
  "$w=New-Object -ComObject WScript.Shell;" ^
  "$s=$w.CreateShortcut($d+'\Sport Gym Acceso.lnk');" ^
  "$s.TargetPath='%DEST%\Sport Gym Acceso.exe';" ^
  "$s.WorkingDirectory='%DEST%';" ^
  "$icon='%DEST%\SportGym.ico';" ^
  "if (Test-Path $icon) { $s.IconLocation = $icon + ',0' };" ^
  "$s.Save()"
echo Icono del acceso directo actualizado.
pause
