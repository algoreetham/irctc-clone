#!/bin/bash
set -e

# Detect Java runtime
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    export JAVA_HOME="$JAVA_HOME"
elif [ -x "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
fi

GRADLE_BIN=""
if [ -x "/Users/preethamsinghsp/.gradle/wrapper/dists/gradle-8.10-bin/deqhafrv1ntovfmgh0nh3npr9/gradle-8.10/bin/gradle" ]; then
    GRADLE_BIN="/Users/preethamsinghsp/.gradle/wrapper/dists/gradle-8.10-bin/deqhafrv1ntovfmgh0nh3npr9/gradle-8.10/bin/gradle"
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="gradle"
fi

if [ -z "$GRADLE_BIN" ]; then
    echo "Gradle not found. Falling back to lightweight WebServer..."
    exec ./run_web.sh "$@"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Launching Spring Boot application via Gradle ($GRADLE_BIN)..."
"$GRADLE_BIN" bootRun "$@"
