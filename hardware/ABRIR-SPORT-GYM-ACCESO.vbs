' Sport Gym Acceso — lanzador oficial (Electron o navegador + lector COM3)
Option Explicit

Dim sh, fso, dest, gw, app, url, profile, edge, chrome, args, errMsg

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

' Sin Electron: pantalla /acceso en Edge o Chrome (modo app)
url = "https://sportgymr10.com/acceso"
profile = sh.ExpandEnvironmentStrings("%LOCALAPPDATA%\SportGymAcceso\EdgeProfile")
If Not fso.FolderExists(profile) Then fso.CreateFolder profile
args = "--app=" & url & " --window-size=1400,900 --disable-features=TranslateUI --user-data-dir=""" & profile & """"

edge = sh.ExpandEnvironmentStrings("%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe")
If fso.FileExists(edge) Then
  sh.Run """" & edge & """ " & args, 1, False
  WScript.Quit 0
End If

edge = sh.ExpandEnvironmentStrings("%ProgramFiles%\Microsoft\Edge\Application\msedge.exe")
If fso.FileExists(edge) Then
  sh.Run """" & edge & """ " & args, 1, False
  WScript.Quit 0
End If

chrome = sh.ExpandEnvironmentStrings("%ProgramFiles%\Google\Chrome\Application\chrome.exe")
If fso.FileExists(chrome) Then
  sh.Run """" & chrome & """ --app=" & url & " --window-size=1400,900", 1, False
  WScript.Quit 0
End If

chrome = sh.ExpandEnvironmentStrings("%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe")
If fso.FileExists(chrome) Then
  sh.Run """" & chrome & """ --app=" & url & " --window-size=1400,900", 1, False
  WScript.Quit 0
End If

sh.Run url, 1, False
