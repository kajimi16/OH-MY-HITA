#!/usr/bin/env python3
"""镜像 OH-MY-HITA 的全部 GitHub Release：APK + 元数据 + 汇总索引。

由 systemd timer 定时触发；只读公开 API，不需要任何凭据。
"""
import hashlib
import json
import os
import sys
import tempfile
import urllib.error
import urllib.request
from datetime import datetime, timezone

REPO = "kajimi16/OH-MY-HITA"
API = f"https://api.github.com/repos/{REPO}/releases"
ROOT = "/srv/oh-my-hita/releases"
META_DIR = os.path.join(ROOT, "meta")
UA = "oh-my-hita-mirror/1.0 (+https://ani2-ext.kajimi.cc/hita/)"


def log(msg):
    print(f"[{datetime.now(timezone.utc).isoformat(timespec='seconds')}] {msg}", flush=True)


def api_get(url):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "application/vnd.github+json",
    })
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def write_atomic(path, data):
    d = os.path.dirname(path)
    fd, tmp = tempfile.mkstemp(dir=d, prefix=".tmp-")
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        os.chmod(tmp, 0o644)
        os.replace(tmp, path)
    except BaseException:
        if os.path.exists(tmp):
            os.unlink(tmp)
        raise


def download(url, dest):
    """下载到 dest.part，校验大小后原子替换。返回 sha256。"""
    tmp = dest + ".part"
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=120) as r, open(tmp, "wb") as f:
        while True:
            chunk = r.read(1 << 20)
            if not chunk:
                break
            f.write(chunk)
    os.chmod(tmp, 0o644)
    os.replace(tmp, dest)
    return sha256_file(dest)


def pick_apk(release):
    assets = [a for a in release.get("assets", []) if a["name"].endswith(".apk")]
    if not assets:
        return None
    return max(assets, key=lambda a: a["size"])


def main():
    os.makedirs(META_DIR, exist_ok=True)
    releases = []
    page = 1
    while True:
        batch = api_get(f"{API}?per_page=100&page={page}")
        if not batch:
            break
        releases.extend(batch)
        if len(batch) < 100:
            break
        page += 1

    index = []
    for rel in releases:
        if rel.get("draft"):
            continue
        tag = rel["tag_name"]
        asset = pick_apk(rel)
        entry = {
            "tag": tag,
            "name": rel.get("name"),
            "published_at": rel.get("published_at"),
            "prerelease": bool(rel.get("prerelease")),
            "html_url": rel.get("html_url"),
        }

        write_atomic(os.path.join(META_DIR, f"{tag}.json"),
                     json.dumps(rel, ensure_ascii=False, indent=2).encode())

        if asset:
            dest = os.path.join(ROOT, asset["name"])
            need = not (os.path.exists(dest)
                        and os.path.getsize(dest) == asset["size"])
            if need:
                log(f"download {tag}: {asset['name']} ({asset['size']} bytes)")
                try:
                    digest = download(asset["browser_download_url"], dest)
                except urllib.error.URLError as e:
                    log(f"FAILED {tag}: {e}")
                    entry["error"] = str(e)
                    index.append(entry)
                    continue
            else:
                digest = sha256_file(dest)
            entry["apk"] = {
                "name": asset["name"],
                "size": os.path.getsize(dest),
                "sha256": digest,
                "url": f"https://ani2-ext.kajimi.cc/hita/releases/{asset['name']}",
            }
        else:
            log(f"{tag}: no apk asset")

        index.append(entry)

    # 最新稳定版指针
    stable = [e for e in index if e.get("apk") and not e["prerelease"]]
    latest = max(stable, key=lambda e: e["published_at"] or "") if stable else None
    if latest:
        src = os.path.join(ROOT, latest["apk"]["name"])
        dst = os.path.join(ROOT, "oh-my-hita-latest.apk")
        tmp = dst + ".part"
        with open(src, "rb") as f, open(tmp, "wb") as g:
            for chunk in iter(lambda: f.read(1 << 20), b""):
                g.write(chunk)
        os.chmod(tmp, 0o644)
        os.replace(tmp, dst)
        log(f"latest -> {latest['tag']} ({latest['apk']['name']})")

    index.sort(key=lambda e: e.get("published_at") or "", reverse=True)
    payload = {
        "repo": REPO,
        "synced_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "releases": index,
    }
    write_atomic(os.path.join(ROOT, "index.json"),
                 json.dumps(payload, ensure_ascii=False, indent=2).encode())
    log(f"synced {len(index)} releases")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:  # noqa: BLE001 - systemd 需要非零退出
        log(f"ERROR {type(e).__name__}: {e}")
        sys.exit(1)
