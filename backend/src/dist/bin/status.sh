#!/bin/bash
APP_HOME="$HOME/.webetl"
PID_FILE="$APP_HOME/webetl.pid"

echo "WebETL Status"
echo "============"
echo "App Home: $APP_HOME"

# Detect OS type
OS_TYPE=$(uname -s)

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null; then
        echo "Status: Running"
        echo "PID: $PID"
        
        # Get memory usage
        case "$OS_TYPE" in
            "Darwin") # MacOS
                MEM=$(ps -o rss= -p $PID | awk '{printf "%.1f", $1/1024}')
                ;;
            "Linux")
                MEM=$(ps -o rss= -p $PID | awk '{printf "%.1f", $1/1024}')
                ;;
            *)
                MEM="unknown"
                ;;
        esac
        echo "Memory: $MEM MB"
        
        # Get uptime
        case "$OS_TYPE" in
            "Darwin") # MacOS
                STARTED=$(ps -o lstart= -p $PID)
                ;;
            "Linux")
                STARTED=$(ps -o lstart= -p $PID)
                ;;
            *)
                STARTED="unknown"
                ;;
        esac
        echo "Started: $STARTED"
        
        # Check if port 8085 is listening
        PORT_CHECK=1
        case "$OS_TYPE" in
            "Darwin") # MacOS
                lsof -i :8085 > /dev/null 2>&1
                PORT_CHECK=$?
                ;;
            "Linux")
                netstat -tuln 2>/dev/null | grep -q ":8085 "
                PORT_CHECK=$?
                ;;
        esac
        
        if [ $PORT_CHECK -eq 0 ]; then
            echo "Port 8085: Listening"
        else
            echo "Port 8085: Not listening (warning)"
        fi
        
        # Show log file size
        if [ -f "$APP_HOME/logs/webetl.log" ]; then
            case "$OS_TYPE" in
                "Darwin") # MacOS
                    LOG_SIZE=$(du -h "$APP_HOME/logs/webetl.log" | awk '{print $1}')
                    ;;
                "Linux")
                    LOG_SIZE=$(du -h "$APP_HOME/logs/webetl.log" | cut -f1)
                    ;;
                *)
                    LOG_SIZE="unknown"
                    ;;
            esac
            echo "Log size: $LOG_SIZE"
        fi
    else
        echo "Status: Not running (stale PID file)"
        rm "$PID_FILE"
    fi
else
    # Try to find process even without PID file
    case "$OS_TYPE" in
        "Darwin") # MacOS
            PID=$(ps aux | grep '[j]ava.*lib/backend.jar' | awk '{print $2}')
            ;;
        "Linux")
            PID=$(ps aux | grep '[j]ava.*lib/backend.jar' | awk '{print $2}')
            ;;
        *)
            PID=""
            ;;
    esac
    if [ ! -z "$PID" ]; then
        echo "Status: Running (PID file missing)"
        echo "PID: $PID"
    else
        echo "Status: Not running"
    fi
fi 