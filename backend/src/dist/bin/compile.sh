#!/bin/bash
APP_HOME="$HOME/.webetl"
mkdir -p "$APP_HOME"
cd "$(dirname "$0")/.."

if [ "$#" -ne 2 ]; then
    echo "Usage: compile.sh <input-sheet.json> <output.jar>"
    exit 1
fi

java -cp lib/backend.jar com.example.compiler.FlowCompilerCLI "$1" "$2" 