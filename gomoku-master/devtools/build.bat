@echo off
rem set solution dir variable
set sln=%cd%\..
rem set modules
set module=java.base,java.desktop
rem if bin dir not exist
IF NOT EXIST %sln%\bin (
    mkdir %sln%\bin
)
IF EXIST %sln%\build (
    rmdir /s /q %sln%\build
)
rem compile java and move binary file
cd %sln%\src && javac -encoding UTF-8 *.java -d ..\bin
rem link module if mods exist
cd %sln% && jlink --no-header-files --no-man-pages --strip-debug -p "bin" --add-modules=%module% --compress=2 --output build
rem establish jar file
cd %sln%\bin && jar cvfe ..\Gomoku.jar Main *
rem back to current dir
cd %sln%\devtools