@echo off
set APP_HOME=%USERPROFILE%\.webetl
set PID_FILE=%APP_HOME%\webetl.pid

echo WebETL Status
echo ============
echo App Home: %APP_HOME%

if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    
    rem Check if process is running
    tasklist /FI "PID eq %PID%" 2>NUL | find /I "%PID%" >NUL
    if not errorlevel 1 (
        echo Status: Running
        echo PID: %PID%
        
        rem Get memory usage using tasklist
        for /f "tokens=5" %%a in ('tasklist /FI "PID eq %PID%" /FO LIST ^| find "Mem Usage"') do (
            echo Memory: %%a
        )
        
        rem Check if port 8085 is listening
        netstat -an | find ":8085" | find "LISTENING" >NUL
        if not errorlevel 1 (
            echo Port 8085: Listening
        ) else (
            echo Port 8085: Not listening (warning)
        )
        
        rem Show log file size
        if exist "%APP_HOME%\logs\webetl.log" (
            for %%I in ("%APP_HOME%\logs\webetl.log") do (
                echo Log size: %%~zI bytes
            )
        )
    ) else (
        echo Status: Not running (stale PID file)
        del "%PID_FILE%"
    )
) else (
    rem Try to find process even without PID file
    tasklist /FI "IMAGENAME eq java.exe" /FI "WINDOWTITLE eq WebETL" 2>NUL | find "java.exe" >NUL
    if not errorlevel 1 (
        echo Status: Running (PID file missing)
        for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FI "WINDOWTITLE eq WebETL" /NH') do (
            echo PID: %%a
        )
    ) else (
        echo Status: Not running
    )
) 