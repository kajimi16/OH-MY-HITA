CREATE TABLE IF NOT EXISTS usage_event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  install_id TEXT NOT NULL,
  event TEXT NOT NULL,
  screen TEXT,
  plugin_id TEXT,
  app_version TEXT NOT NULL DEFAULT '',
  sdk INTEGER NOT NULL DEFAULT 0,
  timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_usage_event_plugin_time
  ON usage_event(plugin_id, event, timestamp);
CREATE INDEX IF NOT EXISTS idx_usage_event_install
  ON usage_event(install_id, timestamp);
