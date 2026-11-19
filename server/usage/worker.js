/**
 * Cloudflare Worker 参考实现：/api/usage 与 /api/market/stats。
 *
 * 环境变量：D1 binding `DB`。
 * 该路由应部署在与现有 HITA API 同域（api.hita.limpu.cn）下。
 */
const ALLOWED = new Set([
  'app_open', 'screen_view', 'plugin_open', 'plugin_install',
  'plugin_market_view', 'timetable_open', 'today_open', 'more_open',
]);

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/api/usage' && request.method === 'POST') {
      return ingest(request, env);
    }
    if (url.pathname === '/api/market/stats' && request.method === 'GET') {
      return stats(url, env);
    }
    return json({ error: 'not_found' }, 404);
  },
};

async function ingest(request, env) {
  const body = await request.json().catch(() => null);
  const events = Array.isArray(body?.events) ? body.events : [];
  if (events.length === 0 || events.length > 200) return json({ error: 'bad_events' }, 400);
  const stmt = env.DB.prepare(
    `INSERT INTO usage_event
      (install_id, event, screen, plugin_id, app_version, sdk, timestamp)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  );
  const batch = [];
  for (const e of events) {
    if (!ALLOWED.has(e?.event)) continue;
    if (typeof e.install_id !== 'string' || e.install_id.length > 80) continue;
    if (typeof e.timestamp !== 'number') continue;
    batch.push(stmt.bind(
      e.install_id,
      e.event,
      typeof e.screen === 'string' ? e.screen.slice(0, 80) : null,
      typeof e.plugin_id === 'string' ? e.plugin_id.slice(0, 160) : null,
      typeof e.app_version === 'string' ? e.app_version.slice(0, 40) : '',
      Number.isInteger(e.sdk) ? e.sdk : 0,
      e.timestamp,
    ));
  }
  if (batch.length === 0) return json({ ok: true, accepted: 0 });
  await env.DB.batch(batch);
  return json({ ok: true, accepted: batch.length });
}

async function stats(url, env) {
  const ids = (url.searchParams.get('ids') || '').split(',').filter(Boolean).slice(0, 50);
  if (ids.length === 0) return json({ stats: {} });
  const placeholders = ids.map(() => '?').join(',');
  const since = Date.now() - 30 * 24 * 3600 * 1000;
  const rows = await env.DB.prepare(
    `SELECT plugin_id,
       SUM(CASE WHEN event = 'plugin_install' THEN 1 ELSE 0 END) AS downloads,
       COUNT(DISTINCT CASE WHEN event = 'plugin_open' AND timestamp >= ? THEN install_id END) AS active_users
     FROM usage_event
     WHERE plugin_id IN (${placeholders})
     GROUP BY plugin_id`
  ).bind(since, ...ids).all();
  const out = {};
  for (const r of rows.results || []) {
    out[r.plugin_id] = { downloads: r.downloads || 0, activeUsers: r.active_users || 0 };
  }
  return json({ stats: out });
}

function json(value, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8' },
  });
}
