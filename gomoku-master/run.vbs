Set oShell = CreateObject ("Wscript.Shell") 
oshell.CurrentDirectory = "./devtools"
Dim strArgs
strArgs = "cmd /c run.bat"
oShell.Run strArgs, 0, false