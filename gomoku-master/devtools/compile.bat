@echo off
rem set solution dir variable
set sln=%cd%\..
rem if bin dir not exist
IF NOT EXIST %sln%\bin (
    mkdir %sln%\bin
)
IF NOT EXIST %sln%\build (
    rem exit if build exist
    echo Cannot release because build no exist, need to rebuild!
) ELSE (
    rem compile java and move binary file
    cd %sln%\src && javac -encoding UTF-8 *.java -d ..\bin
    rem establish jar file
    cd %sln%\bin && jar cvfe ..\Gomoku.jar Main *
    rem run the jar file
    cd %sln% && %sln%\build\bin\java -jar Gomoku.jar
    rem back to current dir
    cd %sln%\devtools
)