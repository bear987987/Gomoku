@echo off
rem set solution dir variable
set sln=%cd%\..
IF NOT EXIST %sln%\build (
    echo Cannot run because build no exist, need to rebuild!
    pause
) ELSE (
    rem run the jar file
    cd %sln% && %sln%\build\bin\java -jar Gomoku.jar
    rem back to current dir
    cd %sln%\devtools
)