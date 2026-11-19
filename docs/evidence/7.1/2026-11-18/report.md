# 7.1 UI evidence — 2026-11-18

> Exploratory only. Upstream is logged out and shows five tabs (资讯 hidden). Our shell intentionally keeps six tabs, so the dock is excluded from these ratios. This is not a formal acceptance baseline.

## Environment

- Device: `emulator-5554`
- Resolution: 1080×2400, density 420
- Font scale: 1.0, light theme
- Upstream: `cn.limpu.hita` 3.1.2 (`2026103101`)
- Ours: `io.github.kajimi16.ohmyhita` 0.7.1-dev (`1795005613`)

## Results

| Page | State | Content diff | Note |
|---|---|---:|---|
| 今日 | logged out, no courses | 5.358% | Header card structure/colors now close; remaining glyph/icon edge differences |
| 待办 | logged out, empty | 3.540% | Content nearly aligned; remaining text raster differences |
| 助手 | logged out, empty conversation | 3.058% | Title/icon aligned; input dock still lacks the attachment button |

Crop: top 130 px / bottom 310 px, tolerance 10. The bottom crop removes the floating dock.

## Synthetic timetable check

- Fixture: `fixtures/synthetic.ics.txt` (fake courses only)
- Our header: title 116 vs upstream 117; chip x 376 vs 375; month/date 343 vs 341
- No pixel ratio: upstream has a different timetable data set
- Screenshot: `ours/timetable__synthetic.png`

## Source of truth

- Screenshots: `upstream/`, `ours/`
- Heatmaps: `diff/`
- Hashes and exact commands metadata: `manifest.json`

## Remaining

- Repeat with a logged-in upstream (6 tabs) for a full-screen comparison.
- Compare the dock as its own region.
- Continue with 时间表, 助手, 资讯, 更多.

## 2026-11-18 登录态更新（补充）

上游已通过 API 登录（API 登录不走 MFA；Web 登录才走短信/OTP）。最新内容区差异：

| 页面 | 差异 | 结论 |
|---|---:|---|
| 待办 | 0.792% | 内容通过；header 2.12%、dock 8.48%，页面未正式通过 |
| 今日 | 1.733% | 内容通过；关键区未测 |
| 助手 | 0.934% | 内容通过；关键区未测 |
| 资讯 | 11.49% | 未通过 |
| 更多 | 6.25% | 未通过（第二模拟器等价未登录/0 课表） |
| 时间表 | 11.46% | 未通过（第 12 周合成 ICS，等价数据） |

本轮未提交上游截图：包含姓名/学号/手机号等个人信息。上面的旧 manifest/截图仍是登录前探索基线，不能替代登录态证据包；资讯/更多/时间表截图与 header/dock 分区比例待补。

## 仍未完成

- 待办多条目/混合完成、今日无课/多课/跨节、时间表周切换、助手长会话/资源卡片、资讯空态/失败态、更多登录态、深色/字体缩放的对比。
- header/dock 独立 ≤1% 阈值；`visual_diff.py` 分区输出。
- HITA-L 三页回归、M5、0.7.1 发布候选。


## 验收决定（2026-11-18）

用户确认：在没有可靠、非过拟合改动的前提下，接受当前残差，停止 header/dock 像素调优。M4 关闭；不声明所有页面达到 2%/1% 阈值。后续 backlog 见 7.1 文档 §8.2。


## 附件解析验证（2026-11-19）

设备 `emulator-5554`，选中附件后输入框实际出现：

- PDF：`【PDF文档】页数：1 … PDF attachment test content`
- DOCX：`【Word文档】DOCX attachment test content`
- XLSX：`【Excel表格】工作表1：Sheet1 第1行：[XLSX attachment test content]`
- PPTX：`【PowerPoint演示文稿】幻灯片1：PPTX attachment test content`

未验证：图片/视频多模态（需要模型 Key）。


## 深色模式冒烟（2026-11-19）

- `cmd uimode night yes` 后六页可读：待办、今日、时间表、助手、资讯、更多。
- 资讯页白底问题已修复。
- 切回浅色后资讯页与改动前截图 0.000% 差异。
- 未做上游深色逐页像素比例。


## 深色/字体验证修正（2026-11-19）

- 深色截图必须用宿主 `PresetShellActivity`；此前一批截图实际是上游 App，已废弃。
- 修复 `shellSecondary/shellTertiary` 自递归崩溃后，宿主 dark More/Today/News 实际可读。
- 字体 0.85/1.3 六页冒烟；1.3 下 More 行高改为 `heightIn(min=...)` 后不再重叠。
- 修复提交：`60b6aa7`。


## 媒体附件路由（2026-11-19）

选择 `wp-test.png` 后：

- 用户消息：`请分析这个附件` + `[附件: wp-test.png]`
- 无 Key 时助手：`出错了：未配置智谱 API Key`

说明选择/编码/请求链路已接通；实际视觉理解仍未验收，需在 AI 设置填写智谱 Key。


## 媒体 Key 测试（2026-11-19）

- 在应用 AI 设置写入测试 Key（未写入仓库/日志）。
- 首次请求 400：定位为 `encodeMedia = { "" }`，已改为真实 Base64。
- 修复后请求返回 **HTTP 429**（限流/额度），等待冷却后重试仍是 429。
- 因此只证明了 Key 生效、请求合法，未取得实际图片理解回复。


## 媒体实际理解（2026-11-19）

- 图片 `wp-test.png`：模型返回“纯蓝色背景图，无其他图案/文字/图形”；截图 `docs/evidence/7.1/2026-11-19/ours/media_image_success.png`。
- 视频 `attach_test.mp4`：连续返回 HTTP 429；视频理解未验收。
- 结论：图片链路通过；视频请求合法但被限流。


## B2/C2 验收口径（2026-11-19）

- B2 接受仅图片；视频 provider-429 为已知缺口。
- C2 接受当前状态矩阵；资讯空态、更多登录态、大量文本长会话未覆盖。
- 不以“全量通过”表述。
