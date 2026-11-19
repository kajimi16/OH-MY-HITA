#!/usr/bin/env bash
# debug 期直接打开运行时界面插件（跳过导航）。
# 用法：./scripts/dev-open-ui-plugin.sh <plugin-id> [query]
set -euo pipefail

ID="${1:?usage: dev-open-ui-plugin.sh <plugin-id> [query]}"
QUERY="${2:-}"
PKG="${PKG:-io.github.kajimi16.ohmyhita}"
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"

$ADB shell am start -n "$PKG/com.stupidtree.hitax.ui.ActivitySplash" >/dev/null
sleep 1
$ADB shell am broadcast \
  -a com.stupidtree.hitax.DEV_OPEN_UI_PLUGIN \
  -n "$PKG/com.stupidtree.hitax.uiplugin.DevUiPluginOpenReceiver" \
  --es id "$ID" \
  --es query "$QUERY" >/dev/null
