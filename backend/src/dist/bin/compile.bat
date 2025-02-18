@echo off
set APP_HOME=%USERPROFILE%\.webetl
if not exist "%APP_HOME%" mkdir "%APP_HOME%"
cd /d "%~dp0\.."

if "%~1" == "" (
    echo Usage:
    echo   List sheets:  compile.bat list
    echo   Compile:      compile.bat compile input-sheet.json output.jar [--verbose]
    exit /b 1
)

java -jar lib\compiler.jar %* 