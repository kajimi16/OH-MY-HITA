# 2026-11-19 状态矩阵与字体缩放证据

设备：`emulator-5554`，1080×2400，420dpi。
截图仅宿主 `io.github.kajimi16.ohmyhita`，不含账号/学号/手机号。

## 字体缩放

| 页面 | font_scale 0.85 | 1.0 | 1.3 |
|---|---|---|---|
| 待办 | — | 已有 | `ours/host13_todo.png` |
| 今日 | — | 已有 | `ours/host13_today.png` |
| 时间表 | — | 已有 | `ours/host13_timetable.png` |
| 助手 | — | 已有 | `ours/host13_agent.png` |
| 资讯 | `ours/host085_news.png` | `ours/light_news_final.png` | `ours/host13_news.png` |
| 更多 | `ours/host085_more.png` | 已有 | `ours/host13_more2.png` |

1.3 下 More 曾出现固定行高重叠，改为 `heightIn(min=...)` 后重抓 `host13_more2.png`。

## 深色

| 页面 | 截图 |
|---|---|
| 更多 | `dark/dark_host_more.png` |
| 今日 | `dark/dark_host_today.png` |
| 资讯 | `dark/dark_host_news.png` |

## 状态覆盖

| 页面 | 已覆盖 | 未覆盖 |
|---|---|---|
| 待办 | 空列表；3 条以上；完成/未完成混合（1/4）；字体 1.3 | — |
| 今日 | 无课（`ours/today_empty.png`）；单课；多课/跨节两门（`ours/today_multi.png`）；字体 1.3 | — |
| 时间表 | 第 12 周等价 ICS；空课表（`ours/timetable_empty.png`）；字体 1.3 | 周切换后课程态 |
| 助手 | 空会话；图片实际理解成功（`ours/media_image_success.png`）；多消息（`ours/agent_multi.png`） | 大量文本长会话；视频理解（429） |
| 资讯 | 列表；0.85/1.0/1.3；深色；加载失败（断网）→恢复网络后重新加载 | 空态 |
| 更多 | 0/1 课表；字体 0.85/1.3；深色 | 登录态 |

结论：字体缩放和深色证据已归档；状态矩阵仍不是全量，未覆盖项已列出，不做“完整通过”声明。
