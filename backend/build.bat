@echo off
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -d out --release 11 @sources.txt
del sources.txt
echo Build complete.
