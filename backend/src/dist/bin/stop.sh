#!/bin/bash
APP_HOME="$HOME/.webetl"
PID_FILE="$APP_HOME/webetl.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null; then
        echo "Stopping WebETL (PID: $PID)..."
        kill $PID
        rm "$PID_FILE"
    else
        echo "Process not found. Cleaning up PID file..."
        rm "$PID_FILE"
    fi
else
    # Fallback: try to find and kill the process
    PID=$(ps aux | grep '[j]ava.*backend.jar' | awk '{print $2}')
    if [ ! -z "$PID" ]; then
        echo "Stopping WebETL (PID: $PID)..."
        kill $PID
    else
        echo "WebETL process not found"
    fi
fi 