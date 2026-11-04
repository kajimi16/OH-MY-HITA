#!/usr/bin/env bash
# 把发行包与插件市场上传到镜像服务器。
# 用法：SSH_KEY=~/.ssh/xxx ./scripts/upload-mirror.sh <apk> <version>
set -euo pipefail

APK="${1:?usage: upload-mirror.sh <apk> <version>}"
VERSION="${2:?usage: upload-mirror.sh <apk> <version>}"
HOST="${MIRROR_HOST:-root@45.144.136.168}"
KEY="${SSH_KEY:-$HOME/.ssh/ani2ani_ed25519}"
REMOTE=/srv/oh-my-hita

scp -i "$KEY" "$APK" "$HOST:$REMOTE/releases/oh-my-hita-$VERSION.apk"
ssh -i "$KEY" "$HOST" "cp $REMOTE/releases/oh-my-hita-$VERSION.apk $REMOTE/releases/oh-my-hita-latest.apk"

scp -i "$KEY" market/index.json market/hello.dex "$HOST:$REMOTE/market/" 2>/dev/null || true
echo "mirror updated: https://ani2-ext.kajimi.cc/hita/releases/oh-my-hita-latest.apk"
