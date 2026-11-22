#!/usr/bin/env bash
set -eu

# Capture first-run onboarding: clear app data, launch, wait, screenshot.
# Usage: scripts/capture-onboarding.sh <output-dir>
ADB="${ADB:-adb}"
PKG="${PKG:-io.github.kajimi16.ohmyhita}"
OUT="${1:-app/build/research/onboarding}"
mkdir -p "$OUT"

"$ADB" shell pm clear "$PKG" >/dev/null
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4
"$ADB" exec-out screencap -p > "$OUT/intro.png"
echo "saved $OUT/intro.png"
