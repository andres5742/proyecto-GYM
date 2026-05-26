' Sport Gym Acceso — ventana tipo app desde sportgymr10.com + lector COM3
Option Explicit

Dim sh, fso, gw, url, profile, edge, chrome, args

Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

gw = fso.GetParentFolderName(WScript.ScriptFullName) & "\turnstile-gateway"
If Not fso.FolderExists(gw) Then
  MsgBox "Falta la carpeta turnstile-gateway." & vbCrLf & vbCrLf & _
         "Ejecute: INSTALAR-SPORT-GYM-ENTRADA.bat", vbCritical, "Sport Gym Acceso"
  WScript.Quit 1
End If

If Not fso.FileExists(gw & "\iniciar-lector-tarjeta.bat") Then
  MsgBox "Falta iniciar-lector-tarjeta.bat en turnstile-gateway.", vbCritical, "Sport Gym Acceso"
  WScript.Quit 1
End If

' Lector tarjeta (ventana aparte, minimizada al inicio)
sh.Run "cmd /k call """ & gw & "\iniciar-lector-tarjeta.bat""", 2, False
WScript.Sleep 5000

url = "https://sportgymr10.com/acceso"
profile = sh.ExpandEnvironmentStrings("%LOCALAPPDATA%\SportGymAcceso\EdgeProfile")
EnsureFolder profile
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

Sub EnsureFolder(path)
  If fso.FolderExists(path) Then Exit Sub
  Dim parent
  parent = fso.GetParentFolderName(path)
  If Len(parent) > 0 And Not fso.FolderExists(parent) Then
    EnsureFolder parent
  End If
  If Not fso.FolderExists(path) Then
    fso.CreateFolder path
  End If
End Sub
