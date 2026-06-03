#!/bin/bash
set -e
mkdir -p .build
cp main.coni .build/main.coni
COMMIT=$(git rev-parse --short HEAD || echo "unknown")
DATE=$(date +"%Y-%m-%d %H:%M:%S %Z")
MSG=$(git log -1 --format=%s || echo "")
MSG=${MSG//\"/}

sed -i '' "s~(def nuke-commit .*~(def nuke-commit \"$COMMIT\")~g" .build/main.coni
sed -i '' "s~(def nuke-build-time .*~(def nuke-build-time \"$DATE\")~g" .build/main.coni
sed -i '' "s~(def nuke-commit-msg .*~(def nuke-commit-msg \"$MSG\")~g" .build/main.coni

if [ "$BUILD_ALL" = "1" ]; then
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 ./coni-compiler build .build/main.coni -o nuke-mac
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=linux GOARCH=amd64 ./coni-compiler build .build/main.coni -o nuke-linux
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=windows GOARCH=amd64 ./coni-compiler build .build/main.coni -o nuke.exe
    cp nuke-mac nuke
else
    CONI_HOME=${CONI_HOME:-/Users/nico/cool/coni-lang} PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 ./coni-compiler build .build/main.coni -o nuke
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
