#!/usr/bin/env bash
# Record a shell interaction for motion acceptance.
# Usage: scripts/motion_capture.sh <package> <out.mp4> [seconds]
set -euo pipefail

PKG="${1:?usage: motion_capture.sh <package> <out.mp4> [seconds]}"
OUT="${2:?usage: motion_capture.sh <package> <out.mp4> [seconds]}"
SECONDS_TO_RECORD="${3:-6}"
REMOTE="/sdcard/oh-my-hita-motion-$(date +%s).mp4"

ADB="${ADB:-adb}"
command -v "$ADB" >/dev/null || { echo "adb not found" >&2; exit 127; }
mkdir -p "$(dirname "$OUT")"

echo "Recording ${SECONDS_TO_RECORD}s..."
"$ADB" shell screenrecord --time-limit "$SECONDS_TO_RECORD" --bit-rate 12000000 "$REMOTE" &
REC_PID=$!
echo "Perform the interaction now (open/close drawer, press/drag dock, switch tab)."
wait "$REC_PID" || true
"$ADB" pull "$REMOTE" "$OUT" >/dev/null
"$ADB" shell rm -f "$REMOTE"
echo "saved: $OUT"
