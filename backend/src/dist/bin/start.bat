@echo off
set APP_HOME=%USERPROFILE%\.webetl
if not exist "%APP_HOME%" mkdir "%APP_HOME%"
if not exist "%APP_HOME%\logs" mkdir "%APP_HOME%\logs"
cd /d "%~dp0\.."

rem Parse command line arguments
set DEBUG=false
if "%1"=="--debug" set DEBUG=true

echo "Starting WebETL..."

rem Set Java options based on debug flag
set JAVA_OPTS=
if "%DEBUG%"=="true" (
    echo Debug mode enabled - check %APP_HOME%\logs\webetl.log for details
    set JAVA_OPTS=-Dlogging.level.root=DEBUG -Dlogging.level.io.webetl=DEBUG -Dlogging.level.org.springframework.security=DEBUG
)

start /B "WebETL" java %JAVA_OPTS% -jar lib\backend.jar --data.dir="%APP_HOME%" > "%APP_HOME%\logs\console.log" 2>&1
for /f "tokens=2" %%a in ('tasklist /fi "imagename eq java.exe" /fi "windowtitle eq WebETL" /nh') do (
    echo %%a > "%APP_HOME%\webetl.pid"
)
timeout /t 2 /nobreak
start http://localhost:8085 