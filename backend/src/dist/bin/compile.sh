#!/bin/bash
APP_HOME="$HOME/.webetl"
mkdir -p "$APP_HOME"
cd "$(dirname "$0")/.."

if [ "$#" -lt 1 ]; then
    echo "Usage:"
    echo "  List sheets:  compile.sh list"
    echo "  Compile:      compile.sh compile <input-sheet.json> <output.jar> [--verbose]"
    exit 1
fi

java -jar lib/compiler.jar "$@" 