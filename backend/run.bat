@echo off
setlocal
cd /d "%~dp0"
if not exist out\com\playground\Main.class call build.bat
java -cp out com.playground.Main %*
