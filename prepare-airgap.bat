@echo off
REM Prepare an airgap mirror for offline use.
REM Run this script from the nuke distribution directory.
REM It will download all required jars into a .\nuke-mirror folder.
REM You can then zip that folder and transfer it to an offline machine.

SET DIR=%~dp0
cd /d "%DIR%"

echo =^> Preparing airgap mirror...
echo     This may take a few minutes on first run (downloading jars).
"%DIR%nuke.exe" mirror export "%DIR%nuke-mirror"

echo.
echo =^> Done! Mirror created at: %DIR%nuke-mirror
echo     Zip it and transfer to your offline machine.
