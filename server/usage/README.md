# /api/usage 与插件人数统计 Worker

客户端实现：

- `UsageAnalytics`：Room 队列；
- `UsageUploadWorker`：`POST https://hita.kajimi.cc/api/usage`；
- `UsageStatsStore`：默认关闭、首次询问、可清除；
- 市场卡片支持索引/接口返回的 `downloads`、`activeUsers`。

部署（Cloudflare Workers + D1）：

```bash
wrangler d1 create hita-usage
wrangler d1 execute hita-usage --file=server/usage/schema.sql
# 在 wrangler.toml 配置 binding DB = hita-usage
wrangler deploy
```

路由需与现有 HITA API 同域；客户端不会上传账号、学号、姓名、课程内容或自由文本。

## 部署到自己的设备/账号

不需要 HITA 工作组权限。把 Worker 部署到自己的 Cloudflare 账号，或把同一逻辑放到自己的
HTTPS 服务器即可。

```bash
# 自己的 Worker 域名，客户端会请求：
#   https://usage.example.com/api/usage
#   https://usage.example.com/api/market/stats?ids=...
./gradlew :app:assembleDebug \
  -PusageBaseUrl=https://usage.example.com \
  -PpluginIndexUrl=https://your-host.example.com/registry/index.json
```

也可以直接安装构建出的 APK 到自己手机；不依赖 HITA 账号或同一工作组。

注意：

- 生产必须 HTTPS；Android 默认不允许明文 HTTP；
- 自建索引中的插件 URL 必须是你自己可访问的 HTTPS 地址，并更新 sha256；
- 不要把自己的服务伪装成 `api.hita.limpu.cn`，除非你确实拥有该域名/授权；
- 本仓库的公告邮箱、更新地址、预设来源仍是上游项目值，发布自己的分支时应一并替换。

## 方案 B：直接部署到自己的 Linux 服务器

本目录的 `python_server.py` 是无第三方依赖的实现，适合已有 VPS/自有服务器。

```bash
# 服务器上
install -d /opt/ohmyhita-usage /var/lib/ohmyhita-usage
install -m755 server/usage/python_server.py /opt/ohmyhita-usage/
install -m644 server/usage/systemd/ohmyhita-usage.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now ohmyhita-usage
sqlite3 /var/lib/ohmyhita-usage/usage.db < server/usage/schema.sql
```

如果已有 Caddy/Nginx，把 `server/usage/caddy.snippet` 中的两条 `handle` 放到对应站点
（例如 `ani2-ext.kajimi.cc`）的 `reverse_proxy` 之前，然后 `caddy validate`/`caddy reload`。

验证：

```bash
curl https://your-domain/api/usage -X POST -H 'content-type: application/json' \
  -d '{"events":[{"install_id":"test","event":"app_open","timestamp":1700000000000}]}'
curl 'https://your-domain/api/market/stats?ids=com.stupidtree.hita.ui.community'
```
