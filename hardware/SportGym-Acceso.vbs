' Sport Gym Acceso — lanzador compatibilidad (solo app de escritorio)
Option Explicit

Dim sh, fso, root, launcher, app, errMsg

Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

root = fso.GetParentFolderName(WScript.ScriptFullName)
launcher = root & "\ABRIR-SPORT-GYM-ACCESO.vbs"

If fso.FileExists(launcher) Then
  sh.Run "wscript.exe """ & launcher & """", 1, False
  WScript.Quit 0
End If

app = sh.ExpandEnvironmentStrings("%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe")
If Not fso.FileExists(app) Then app = "C:\SportGym\Sport Gym Acceso.exe"
If Not fso.FileExists(app) Then app = sh.ExpandEnvironmentStrings("%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe")
If Not fso.FileExists(app) Then app = sh.ExpandEnvironmentStrings("%ProgramFiles(x86)%\Sport Gym Acceso\Sport Gym Acceso.exe")

If fso.FileExists(app) Then
  sh.Run """" & app & """", 1, False
  WScript.Quit 0
End If

errMsg = "No se encontro Sport Gym Acceso.exe." & vbCrLf & vbCrLf & _
         "Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat para reinstalar."
MsgBox errMsg, vbCritical, "Sport Gym Acceso"
WScript.Quit 1
