#!/usr/bin/env python3
"""Self-hosted /api/usage and /api/market/stats (stdlib only)."""
import json, os, re, sqlite3, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

DB = os.environ.get("USAGE_DB", "/var/lib/ohmyhita-usage/usage.db")
HOST = os.environ.get("USAGE_HOST", "127.0.0.1")
PORT = int(os.environ.get("USAGE_PORT", "8791"))
ALLOWED = {"app_open", "screen_view", "plugin_open", "plugin_install",
           "plugin_market_view", "timetable_open", "today_open", "more_open"}

def db():
    con = sqlite3.connect(DB, timeout=10)
    con.execute("PRAGMA journal_mode=WAL")
    con.execute("""CREATE TABLE IF NOT EXISTS usage_event (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        install_id TEXT NOT NULL, event TEXT NOT NULL, screen TEXT,
        plugin_id TEXT, app_version TEXT NOT NULL DEFAULT '',
        sdk INTEGER NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL)""")
    con.execute("CREATE INDEX IF NOT EXISTS idx_event_plugin_time ON usage_event(plugin_id,event,timestamp)")
    con.execute("CREATE INDEX IF NOT EXISTS idx_event_install ON usage_event(install_id,timestamp)")
    return con

def clean(value, limit):
    return value[:limit] if isinstance(value, str) else None

class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        # no request bodies / IPs in logs
        pass

    def reply(self, code, obj):
        data = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        url = urlparse(self.path)
        if url.path == "/health":
            return self.reply(200, {"ok": True})
        if url.path != "/api/market/stats":
            return self.reply(404, {"error": "not_found"})
        ids = [x for x in (parse_qs(url.query).get("ids", [""])[0]).split(",") if x][:50]
        if not ids:
            return self.reply(200, {"stats": {}})
        since = int(time.time() * 1000) - 30 * 24 * 3600 * 1000
        marks = ",".join("?" for _ in ids)
        con = db()
        try:
            rows = con.execute(
                f"SELECT plugin_id, SUM(CASE WHEN event='plugin_install' THEN 1 ELSE 0 END) d,"
                f" COUNT(DISTINCT CASE WHEN event='plugin_open' AND timestamp>=? THEN install_id END) a"
                f" FROM usage_event WHERE plugin_id IN ({marks}) GROUP BY plugin_id",
                [since, *ids],
            ).fetchall()
        finally:
            con.close()
        return self.reply(200, {"stats": {r[0]: {"downloads": r[1], "activeUsers": r[2]} for r in rows}})

    def do_POST(self):
        if urlparse(self.path).path != "/api/usage":
            return self.reply(404, {"error": "not_found"})
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size <= 0 or size > 256 * 1024:
                return self.reply(400, {"error": "bad_size"})
            body = json.loads(self.rfile.read(size))
            events = body.get("events") or []
            if not isinstance(events, list) or len(events) > 200:
                return self.reply(400, {"error": "bad_events"})
            con = db()
            accepted = 0
            try:
                for e in events:
                    if not isinstance(e, dict) or e.get("event") not in ALLOWED:
                        continue
                    iid = clean(e.get("install_id"), 80)
                    ts = e.get("timestamp")
                    if not iid or not isinstance(ts, (int, float)):
                        continue
                    con.execute(
                        "INSERT INTO usage_event(install_id,event,screen,plugin_id,app_version,sdk,timestamp)"
                        " VALUES(?,?,?,?,?,?,?)",
                        (iid, e["event"], clean(e.get("screen"), 80),
                         clean(e.get("plugin_id"), 160), clean(e.get("app_version"), 40) or "",
                         int(e.get("sdk") or 0), int(ts)),
                    )
                    accepted += 1
                con.commit()
            finally:
                con.close()
            return self.reply(200, {"ok": True, "accepted": accepted})
        except Exception as exc:
            return self.reply(400, {"error": "bad_request"})

if __name__ == "__main__":
    os.makedirs(os.path.dirname(DB), exist_ok=True)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
