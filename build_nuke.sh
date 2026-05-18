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
    CONI_HOME=/Users/nico/cool/coni-lang PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 /tmp/coni-compiler build .build/main.coni -o nuke-mac
    CONI_HOME=/Users/nico/cool/coni-lang PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=linux GOARCH=amd64 /tmp/coni-compiler build .build/main.coni -o nuke-linux
    CONI_HOME=/Users/nico/cool/coni-lang PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 GOOS=windows GOARCH=amd64 /tmp/coni-compiler build .build/main.coni -o nuke.exe
    cp nuke-mac nuke
else
    CONI_HOME=/Users/nico/cool/coni-lang PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 /tmp/coni-compiler build .build/main.coni -o nuke
fi
