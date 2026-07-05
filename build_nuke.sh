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

CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang}
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
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 $COMPILER compile-native .build/main.coni -o .build/mac
    mv .build/mac/main nuke-mac
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=linux GOARCH=amd64 $COMPILER compile-native .build/main.coni -o .build/linux
    mv .build/linux/main nuke-linux
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=windows GOARCH=amd64 $COMPILER compile-native .build/main.coni -o .build/windows
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
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 $COMPILER compile-native .build/main.coni -o .build/dev
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
