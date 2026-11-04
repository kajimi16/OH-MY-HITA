#!/usr/bin/env bash
# 把发行包上传到镜像服务器，并校验远端文件与本地一致。
# 用法：SSH_KEY=~/.ssh/xxx ./scripts/upload-mirror.sh <apk> <version>
set -euo pipefail

APK="${1:?usage: upload-mirror.sh <apk> <version>}"
VERSION="${2:?usage: upload-mirror.sh <apk> <version>}"
HOST="${MIRROR_HOST:-root@45.144.136.168}"
KEY="${SSH_KEY:-$HOME/.ssh/ani2ani_ed25519}"
REMOTE=/srv/oh-my-hita
NAME="oh-my-hita-$VERSION.apk"

[ -f "$APK" ] || { echo "no such apk: $APK" >&2; exit 1; }
LOCAL_SHA=$(sha256sum "$APK" | cut -d' ' -f1)

scp -i "$KEY" "$APK" "$HOST:$REMOTE/releases/$NAME"

REMOTE_SHA=$(ssh -i "$KEY" "$HOST" "sha256sum $REMOTE/releases/$NAME | cut -d' ' -f1")
if [ "$LOCAL_SHA" != "$REMOTE_SHA" ]; then
    echo "sha256 mismatch: local=$LOCAL_SHA remote=$REMOTE_SHA" >&2
    exit 1
fi

ssh -i "$KEY" "$HOST" "cp $REMOTE/releases/$NAME $REMOTE/releases/oh-my-hita-latest.apk"

cat <<MSG
mirror updated
  url:    https://ani2-ext.kajimi.cc/hita/releases/$NAME
  latest: https://ani2-ext.kajimi.cc/hita/releases/oh-my-hita-latest.apk
  sha256: $LOCAL_SHA
MSG
