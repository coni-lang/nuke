#!/bin/bash
set -e
mkdir -p .build
cp main.coni .build/main.coni
COMMIT=$(git rev-parse --short HEAD || echo "unknown")
DATE=$(date +"%Y-%m-%d %H:%M:%S %z")
MSG=$(git log -1 --format=%s || echo "")
MSG=${MSG//\"/}

sed -i.bak "s~(def nuke-commit .*~(def nuke-commit \"$COMMIT\")~g" .build/main.coni
sed -i.bak "s~(def nuke-build-time .*~(def nuke-build-time \"$DATE\")~g" .build/main.coni
sed -i.bak "s~(def nuke-commit-msg .*~(def nuke-commit-msg \"$MSG\")~g" .build/main.coni
rm -f .build/main.coni.bak

if [ -z "$CONI_HOME" ] || [ ! -f "$CONI_HOME/core.coni" ]; then
    echo "[DEBUG] CONI_HOME is not set or missing core.coni. Searching..."
    FOUND_CORE=$(find /home/runner -name "core.coni" 2>/dev/null | head -n 1 || true)
    if [ -n "$FOUND_CORE" ]; then
        export CONI_HOME=$(dirname "$FOUND_CORE")
        echo "[DEBUG] Found core.coni at $FOUND_CORE. Set CONI_HOME=$CONI_HOME"
    else
        export CONI_HOME=/Users/nico/cool/coni-lang
        echo "[DEBUG] Could not find core.coni. Defaulting to $CONI_HOME"
    fi
else
    export CONI_HOME
    echo "[DEBUG] CONI_HOME already set to $CONI_HOME"
fi

if [ -z "$CONI_COMPILER" ]; then
    if [ -f "$CONI_HOME/coni" ]; then
        COMPILER="$CONI_HOME/coni"
    else
        COMPILER="coni"
    fi
else
    COMPILER="$CONI_COMPILER"
fi

if [ "$BUILD_ALL" = "1" ]; then
    mkdir -p .build/mac .build/linux .build/windows
    PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 $COMPILER compile-native .build/main.coni -o .build/mac
    mv .build/mac/main nuke-mac
    PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=linux GOARCH=amd64 $COMPILER compile-native .build/main.coni -o .build/linux
    mv .build/linux/main nuke-linux
    PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=windows GOARCH=amd64 $COMPILER compile-native .build/main.coni -o .build/windows
    mv .build/windows/main.exe nuke.exe 2>/dev/null || mv .build/windows/main nuke.exe
    if [ "$(uname)" = "Linux" ]; then
        cp nuke-linux nuke
    elif [ "$(uname)" = "Darwin" ]; then
        cp nuke-mac nuke
    else
        cp nuke.exe nuke
    fi
else
    mkdir -p .build/dev
    PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 $COMPILER compile-native .build/main.coni -o .build/dev
    mv .build/dev/main nuke
fi

echo "Running smoke test to verify syntax and parsing..."
./nuke version || { echo "Smoke test failed! nuke has syntax errors or runtime issues."; exit 1; }

# Copy to IntelliJ plugin resources
mkdir -p nuke-intellij-plugin/src/main/resources/bin
if [ -f nuke ]; then
    cp nuke nuke-intellij-plugin/src/main/resources/bin/nuke
fi
if [ -f nuke-mac ]; then
    cp nuke-mac nuke-intellij-plugin/src/main/resources/bin/nuke-mac
    cp nuke-mac nuke-intellij-plugin/src/main/resources/bin/nuke
fi
if [ -f nuke-linux ]; then
    cp nuke-linux nuke-intellij-plugin/src/main/resources/bin/nuke-linux
fi
if [ -f nuke.exe ]; then
    cp nuke.exe nuke-intellij-plugin/src/main/resources/bin/nuke.exe
fi
