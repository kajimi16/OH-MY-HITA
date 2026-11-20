#!/usr/bin/env bash
# Build plugin-eas runtime dex, install it, and refresh installed.json sha256.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADB="${ADB:-adb}"
PKG="${PKG:-io.github.kajimi16.ohmyhita}"
PLUGIN_ID=com.stupidtree.hitax.eas

./gradlew :plugin-eas:buildRuntimeDex --console=plain
DEX=plugin-eas/build/runtime-dex/plugin.dex
SHA=$(sha256sum "$DEX" | cut -d' ' -f1)
SIZE=$(stat -c%s "$DEX")

"$ADB" shell am force-stop "$PKG"
"$ADB" push "$DEX" /data/local/tmp/plugin.dex >/dev/null
"$ADB" shell run-as "$PKG" cp /data/local/tmp/plugin.dex "files/runtime-plugins/$PLUGIN_ID/plugin.dex"
"$ADB" shell run-as "$PKG" rm -rf "files/runtime-plugins/$PLUGIN_ID/opt"
"$ADB" shell run-as "$PKG" cat files/runtime-plugins/installed.json > /tmp/eas-installed.json
python3 - "$SHA" "$SIZE" <<'PY'
import json, sys
path = '/tmp/eas-installed.json'
data = json.load(open(path))
for item in data:
    if item['id'] == 'com.stupidtree.hitax.eas':
        item['sha256'] = sys.argv[1]
        item['size'] = int(sys.argv[2])
json.dump(data, open(path, 'w'), separators=(',', ':'))
PY
"$ADB" push /tmp/eas-installed.json /data/local/tmp/installed.json >/dev/null
"$ADB" shell run-as "$PKG" cp /data/local/tmp/installed.json files/runtime-plugins/installed.json
"$ADB" shell rm -f /data/local/tmp/plugin.dex /data/local/tmp/installed.json
rm -f /tmp/eas-installed.json
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
echo "installed $PLUGIN_ID sha256=$SHA size=$SIZE"
