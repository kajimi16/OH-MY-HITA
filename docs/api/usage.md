# /api/usage 与插件人数统计契约

## 官方部署

```text
https://hita.kajimi.cc/api/usage
https://hita.kajimi.cc/api/market/stats
```

## POST /api/usage

请求：

```json
{
  "events": [
    {
      "install_id": "uuid",
      "event": "screen_view",
      "screen": "more",
      "plugin_id": null,
      "app_version": "0.7.2",
      "sdk": 35,
      "timestamp": 1795057000000
    }
  ]
}
```

约束：

- 只接受客户端白名单事件：`app_open`、`screen_view`、`plugin_open`、
  `plugin_install`、`plugin_market_view`、`timetable_open`、`today_open`、`more_open`；
- 服务端不得要求/保存账号、学号、姓名、课程内容、自由文本；
- `install_id` 由客户端随机生成，可在“清除使用统计”后重新生成；
- 成功返回 `200`；失败保留队列，由 WorkManager 重试。

## GET /api/market/stats?ids=...

返回：

```json
{
  "stats": {
    "com.stupidtree.hita.ui.community": {
      "downloads": 12,
      "activeUsers": 4
    }
  }
}
```

市场索引也可直接携带同名可选字段 `downloads`、`activeUsers`；客户端优先展示索引值，
没有则不显示。`activeUsers` 建议按匿名 `install_id` 去重、7/30 天窗口统计。

## 隐私

- 默认关闭；
- 首次启动明确询问；
- 设置页可关闭并清除队列/匿名 id；
- 不接收自由文本字段。
