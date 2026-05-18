#!/bin/bash
set -e
mkdir -p .build
cp main.coni .build/main.coni
COMMIT=$(git rev-parse --short HEAD || echo "unknown")
DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
sed -i '' "s/(def nuke-commit.*/(def nuke-commit \"$COMMIT\")/g" .build/main.coni
sed -i '' "s/(def nuke-build-time.*/(def nuke-build-time \"$DATE\")/g" .build/main.coni
CONI_HOME=/Users/nico/cool/coni-lang PATH="$PATH:/usr/local/go/bin:/opt/homebrew/bin" CGO_ENABLED=0 /tmp/coni-compiler build .build/main.coni -o nuke
