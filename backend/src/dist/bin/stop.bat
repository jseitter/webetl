@echo off
set APP_HOME=%USERPROFILE%\.webetl
set PID_FILE=%APP_HOME%\webetl.pid

if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    taskkill /PID %PID% /F
    del "%PID_FILE%"
) else (
    rem Fallback: try to find and kill the process
    for /f "tokens=2" %%a in ('tasklist /fi "imagename eq java.exe" /fi "windowtitle eq WebETL" /nh') do (
        taskkill /PID %%a /F
    )
) 