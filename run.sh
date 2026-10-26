#!/bin/bash
set -e

# Detect Java runtime
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif [ -x "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java" ]; then
    JAVA="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA="java"
else
    echo "Error: java runtime not found. Please install a JRE/JDK or configure JAVA_HOME."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN_DIR="$SCRIPT_DIR/bin"
LIB_DIR="$SCRIPT_DIR/lib"

if [ ! -d "$BIN_DIR/railway" ]; then
    echo "Compiled binaries not found. Running compile.sh first..."
    bash "$SCRIPT_DIR/compile.sh"
fi

CP="$BIN_DIR"
if [ -f "$LIB_DIR/sqlite-jdbc.jar" ]; then
    CP="$CP:$LIB_DIR/sqlite-jdbc.jar"
fi

cd "$SCRIPT_DIR"
"$JAVA" -cp "$CP" railway.app.RailwayReservationApp "$@"
