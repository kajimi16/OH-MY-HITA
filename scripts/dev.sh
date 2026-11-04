#!/usr/bin/env bash
# 开发期插件环境操作：全部走 adb/广播，不点市场界面。
#
#   ./scripts/dev.sh status
#   ./scripts/dev.sh reset                 # 清空运行时插件/市场缓存并重启
#   ./scripts/dev.sh open <ui-plugin-id> [query]
#   ./scripts/dev.sh install-ui <module|apk>
#   ./scripts/dev.sh install-dex <plugin-id>
#   ./scripts/dev.sh uninstall-dex <plugin-id>
#   ./scripts/dev.sh enable-dex <plugin-id> <true|false>
#   ./scripts/dev.sh logs
set -euo pipefail

PKG="${PKG:-io.github.kajimi16.ohmyhita}"
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

run_as() { $ADB shell run-as "$PKG" "$@" 2>/dev/null || true; }
broadcast() {
  $ADB shell am broadcast --include-stopped-packages "$@" >/dev/null
}
restart_app() {
  $ADB shell am force-stop "$PKG"
  $ADB shell am start -n "$PKG/com.stupidtree.hitax.ui.ActivitySplash" >/dev/null
}

case "${1:-status}" in
  status)
    echo "== 内置插件状态 =="
    run_as cat shared_prefs/plugin_state.xml | sed 's/&quot;/"/g' | grep -o '<string>[^<]*</string>' || echo "(全部启用)"
    echo "== 运行时 dex 插件 =="
    run_as cat files/runtime-plugins/installed.json || echo "(无)"
    echo
    run_as cat files/runtime-plugins/disabled.json || echo "(无停用)"
    echo
    echo "== 界面插件 =="
    run_as cat shared_prefs/ui_plugins.xml | sed 's/&quot;/"/g' | tail -3 || echo "(无)"
    echo "== APK 插件停用 =="
    run_as cat shared_prefs/plugin_state.xml | sed 's/&quot;/"/g' | grep -o 'apk:[^<]*' || echo "(无)"
    ;;
  reset)
    run_as rm -rf files/runtime-plugins files/ui-plugins files/market
    run_as rm -f shared_prefs/ui_plugins.xml shared_prefs/plugin_state.xml
    restart_app
    echo "reset done"
    ;;
  open)
    ID="${2:?usage: dev.sh open <plugin-id> [query]}"
    QUERY="${3:-}"
    $ADB shell am start -n "$PKG/com.stupidtree.hitax.ui.ActivitySplash" >/dev/null
    sleep 1
    if [ -n "$QUERY" ]; then
      broadcast -a com.stupidtree.hitax.DEV_OPEN_UI_PLUGIN \
        -n "$PKG/com.stupidtree.hitax.uiplugin.DevUiPluginOpenReceiver" \
        --es id "$ID" --es query "$QUERY"
    else
      broadcast -a com.stupidtree.hitax.DEV_OPEN_UI_PLUGIN \
        -n "$PKG/com.stupidtree.hitax.uiplugin.DevUiPluginOpenReceiver" --es id "$ID"
    fi
    ;;
  install-ui)
    TARGET="${2:?usage: dev.sh install-ui <module|apk>}"
    if [ -f "$TARGET" ]; then
      SHA=$(sha256sum "$TARGET" | cut -d' ' -f1)
      $ADB push "$TARGET" /data/local/tmp/dev-ui-plugin.apk >/dev/null
      run_as cp /data/local/tmp/dev-ui-plugin.apk "/data/user/0/$PKG/files/dev-ui-plugin.apk"
      broadcast -a com.stupidtree.hitax.DEV_INSTALL_UI_PLUGIN \
        -n "$PKG/com.stupidtree.hitax.uiplugin.DevUiPluginInstallerReceiver" \
        --es path "/data/user/0/$PKG/files/dev-ui-plugin.apk" --es sha256 "$SHA"
    else
      "$ROOT/scripts/dev-install-ui-plugin.sh" "$TARGET"
      exit 0
    fi
    ;;
  install-dex)
    ID="${2:?usage: dev.sh install-dex <plugin-id>}"
    broadcast -a com.stupidtree.hitax.DEV_INSTALL_DEX \
      -n "$PKG/com.stupidtree.hitax.market.DevMarketReceiver" --es id "$ID"
    ;;
  uninstall-dex)
    ID="${2:?usage: dev.sh uninstall-dex <plugin-id>}"
    broadcast -a com.stupidtree.hitax.DEV_UNINSTALL_DEX \
      -n "$PKG/com.stupidtree.hitax.market.DevMarketReceiver" --es id "$ID"
    ;;
  enable-dex)
    ID="${2:?usage: dev.sh enable-dex <plugin-id> <true|false>}"
    ENABLED="${3:?usage: dev.sh enable-dex <plugin-id> <true|false>}"
    broadcast -a com.stupidtree.hitax.DEV_ENABLE_DEX \
      -n "$PKG/com.stupidtree.hitax.market.DevMarketReceiver" \
      --es id "$ID" --ez enabled "$ENABLED"
    ;;
  logs)
    $ADB logcat -s PluginHost:* PluginInstaller:* DevMarket:* DevUiPlugin:* HelloPlugin:* | tail -50
    ;;
  restart)
    restart_app
    ;;
  *)
    echo "unknown command: $1" >&2
    exit 1
    ;;
esac
