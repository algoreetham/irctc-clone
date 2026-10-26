#!/bin/bash
set -e

# Detect Java compiler
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
elif [ -x "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javac" ]; then
    JAVAC="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javac"
elif command -v javac >/dev/null 2>&1; then
    JAVAC="javac"
else
    echo "Error: javac not found. Please install a Java JDK or configure JAVA_HOME."
    exit 1
fi

echo "Using Java compiler: $($JAVAC -version 2>&1)"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/bin"
LIB_DIR="$SCRIPT_DIR/lib"
DATA_DIR="$SCRIPT_DIR/data"

mkdir -p "$BIN_DIR"
mkdir -p "$DATA_DIR"

# Build classpath
CP="$BIN_DIR"
if [ -f "$LIB_DIR/sqlite-jdbc.jar" ]; then
    CP="$CP:$LIB_DIR/sqlite-jdbc.jar"
fi

echo "Compiling Java core application, JDBC repository, and built-in WebServer..."
find "$SRC_DIR" -name "*.java" ! -path "*/spring/*" ! -name "RailwayWebApplication.java" > "$BIN_DIR/sources.txt"
"$JAVAC" -cp "$CP" -d "$BIN_DIR" @"$BIN_DIR/sources.txt"
rm -f "$BIN_DIR/sources.txt"

echo "Compilation successful! Bytecode output to $BIN_DIR"
