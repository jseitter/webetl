@echo off
set APP_HOME=%USERPROFILE%\.webetl
if not exist "%APP_HOME%" mkdir "%APP_HOME%"
cd /d "%~dp0\.."

if "%~2"=="" (
    echo Usage: compile.bat input-sheet.json output.jar
    exit /b 1
)

java -cp lib\backend.jar com.example.compiler.FlowCompilerCLI %1 %2 