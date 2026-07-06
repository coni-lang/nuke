#!/bin/sh
# Prepare an airgap mirror for offline use.
# Run this script from the nuke distribution directory.
# It will download all required jars into a ./nuke-mirror folder.
# You can then zip that folder and transfer it to an offline machine.

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

if [ "$(uname)" = "Darwin" ]; then
  NUKE="$DIR/nuke-mac"
elif [ "$(uname)" = "Linux" ]; then
  NUKE="$DIR/nuke-linux"
else
  echo "Use prepare-airgap.bat on Windows"
  exit 1
fi

echo "==> Preparing airgap mirror..."
echo "    This may take a few minutes on first run (downloading jars)."
"$NUKE" mirror export "$DIR/nuke-mirror"
echo ""
echo "==> Done! Mirror created at: $DIR/nuke-mirror"
echo "    Zip it and transfer to your offline machine:"
echo "    zip -r nuke-mirror.zip nuke-mirror/"
