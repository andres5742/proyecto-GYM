' Sport Gym Acceso — lanzador oficial (solo .exe + lector COM3)
Option Explicit

Dim sh, fso, dest, gw, app, errMsg

Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

dest = "C:\SportGym\"
If Not fso.FolderExists(dest) Then dest = fso.GetParentFolderName(WScript.ScriptFullName) & "\"
If Right(dest, 1) <> "\" Then dest = dest & "\"

gw = dest & "turnstile-gateway"

If Not fso.FileExists(gw & "\iniciar-lector-tarjeta.bat") Then
  errMsg = "Falta el lector en:" & vbCrLf & gw & vbCrLf & vbCrLf & _
           "Ejecute ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat con internet."
  MsgBox errMsg, vbCritical, "Sport Gym Acceso"
  WScript.Quit 1
End If

If fso.FileExists(gw & "\preparar-com3.bat") Then
  sh.Run "cmd /c call """ & gw & "\preparar-com3.bat""", 0, True
End If

' Bloqueo preventivo del torniquete al iniciar (independiente de Python)
If fso.FileExists(gw & "\bloquear-torniquete-arranque.bat") Then
  sh.Run "cmd /c call """ & gw & "\bloquear-torniquete-arranque.bat""", 0, True
ElseIf fso.FileExists(gw & "\turnstile_gate.py") Then
  sh.Run "cmd /c cd /d """ & gw & """ && python turnstile_gate.py startup", 0, True
End If

' Siempre iniciar lector COM3 primero (tarjetas + seguro fisico hi/a)
sh.Run "cmd /k call """ & gw & "\iniciar-lector-tarjeta.bat""", 1, False
WScript.Sleep 8000

app = sh.ExpandEnvironmentStrings("%LOCALAPPDATA%\Programs\Sport Gym Acceso\Sport Gym Acceso.exe")
If Not fso.FileExists(app) Then app = dest & "Sport Gym Acceso.exe"
If Not fso.FileExists(app) Then app = sh.ExpandEnvironmentStrings("%ProgramFiles%\Sport Gym Acceso\Sport Gym Acceso.exe")
If Not fso.FileExists(app) Then app = sh.ExpandEnvironmentStrings("%ProgramFiles(x86)%\Sport Gym Acceso\Sport Gym Acceso.exe")

If fso.FileExists(app) Then
  sh.Run """" & app & """", 1, False
  WScript.Quit 0
End If

errMsg = "No se encontro Sport Gym Acceso.exe." & vbCrLf & vbCrLf & _
         "Para instalar/reparar ejecute:" & vbCrLf & _
         "ACTUALIZAR-TORNIQUETE-DESDE-GIT.bat" & vbCrLf & vbCrLf & _
         "Este lanzador no abre navegador."
MsgBox errMsg, vbCritical, "Sport Gym Acceso"
WScript.Quit 1
