#!/usr/bin/env bash
# 开发期快速安装运行时界面插件（仅 debug 包可用）。
# 用法：./scripts/dev-install-ui-plugin.sh [module] [--no-build]
set -euo pipefail

MODULE="${1:-plugin-ui-resource}"
BUILD=1
[ "${2:-}" = "--no-build" ] && BUILD=0

PKG="${PKG:-io.github.kajimi16.ohmyhita}"
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"

cd "$(dirname "$0")/.."
if [ "$BUILD" = "1" ]; then
  ./gradlew ":$MODULE:assembleDebug" -q
fi

APK=$(find "$MODULE/build/outputs/apk/debug" -name "*.apk" | head -1)
[ -n "$APK" ] || { echo "APK not found for $MODULE" >&2; exit 1; }

META=$(python3 - "$APK" <<'PY'
import json, sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    meta = json.loads(z.read("assets/plugin.json"))
print(meta["id"], meta["version"], meta["entryClass"], sep="\t")
PY
)
ID=$(echo "$META" | cut -f1)
VERSION=$(echo "$META" | cut -f2)
SHA=$(sha256sum "$APK" | cut -d' ' -f1)

$ADB push "$APK" "/data/local/tmp/dev-ui-plugin.apk" >/dev/null
$ADB shell run-as "$PKG" cp /data/local/tmp/dev-ui-plugin.apk /data/user/0/$PKG/files/dev-ui-plugin.apk

$ADB logcat -c
$ADB shell am broadcast \
  -a com.stupidtree.hitax.DEV_INSTALL_UI_PLUGIN \
  -n "$PKG/com.stupidtree.hitax.uiplugin.DevUiPluginInstallerReceiver" \
  --es path "/data/user/0/$PKG/files/dev-ui-plugin.apk" \
  --es sha256 "$SHA" >/dev/null

sleep 2
$ADB logcat -d -s DevUiPlugin:* | tail -3

# 冷启动，让市场与入口看到新版本
$ADB shell am force-stop "$PKG"
$ADB shell am start -n "$PKG/com.stupidtree.hitax.ui.ActivitySplash" >/dev/null 2>&1 || \
  $ADB shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null

echo "installed: $ID@$VERSION sha256=${SHA:0:12}"
