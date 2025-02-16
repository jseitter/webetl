#!/bin/bash
APP_HOME="$HOME/.webetl"
mkdir -p "$APP_HOME"
mkdir -p "$APP_HOME/logs"
cd "$(dirname "$0")/.."

# Parse command line arguments
DEBUG=false
while [ "$1" != "" ]; do
    case $1 in
        --debug )    DEBUG=true
                    ;;
    esac
    shift
done

echo "Starting WebETL..."

# Set Java options based on debug flag
if [ "$DEBUG" = true ] ; then
    echo "Debug mode enabled - check $APP_HOME/logs/webetl.log for details"
    JAVA_OPTS="-Dlogging.level.root=DEBUG -Dlogging.level.io.webetl=DEBUG -Dlogging.level.org.springframework.security=DEBUG"
else
    JAVA_OPTS=""
fi

java $JAVA_OPTS -jar lib/backend.jar --data.dir="$APP_HOME" > "$APP_HOME/logs/console.log" 2>&1 & echo $! > "$APP_HOME/webetl.pid"
sleep 2
if command -v xdg-open >/dev/null 2>&1; then
    xdg-open http://localhost:8085
elif command -v open >/dev/null 2>&1; then
    open http://localhost:8085
fi 