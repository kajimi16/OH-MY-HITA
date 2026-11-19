# HITA 整合包一比一移植状态

> 压缩上下文用的权威状态文件。后续会话先读此文件，不依赖聊天记忆。

## 分支

- 分支：`feat/hita-android-shell`
- 基线：已合并 `origin/master` / `v0.6.9`；已发布 `0.7.0`、`0.7.1`、`0.7.2`
- HITA_Android Shell 已由预设映射激活，固定 6 tab；视觉仍是预览版

## HITA-L 1.5.3

状态：**完成（同源原生模式，不是独立 Shell）**

实现：

- 宿主与 HITA-L 同源，复用原生 `FragmentTimeLine` / `TimetableFragment` / `NavigationFragment`
- `active_preset == hita-l-1.5.3` 时隐藏插件市场 tab
- HITA-L 系统导航栏为黑色

截图证据（同数据、同 hint、同系统栏）：

| 页面 | 差异像素 |
|---|---|
| 今日 | 0.55% |
| 时间表 | 0.53% |
| 功能中心 | 0.47% |

## HITA_Android 3.1.2 Shell

状态：**M4 已按用户确认关闭（2026-11-18）；3 页内容区通过，header/dock 残差已记录并接受**

| 页面 | 实现 | 当前差异 |
|---|---|---:|
| 待办 | 上游 `HomeworkScreen` 结构 | 内容 **0.792%**；header 2.12%、dock 8.48%（未过） |
| 今日 | 真实数据；主线程 Room 查询已改 IO；“全部结束”状态已补 | 内容 **1.733%**；关键区未测 |
| 时间表 | 8:00–20:00 时间轴网格；第 12 周合成 ICS | 11.46%（等价周次/课程） |
| 助手 | 上游布局、Markdown、文本附件 | 内容 **0.934%**；关键区未测 |
| 资讯 | TabRow、LXGW 字体、系列分组 | 11.02%–11.36%（用户接受残差，关闭） |
| 更多 | 上游结构、真实入口 | 6.25%（第二模拟器等价未登录/0 课表） |

Shell 入口：

- `app/.../shell/PresetShellActivity.kt`
- `MainActivity` 在 onCreate/onResume 检测 `activeShell` 后跳转，市场切换后立即生效
- 6 tab：待办 / 今日 / 时间表 / 助手 / 资讯 / 更多
- `hita-android-3.1.2` 的 shell 由宿主稳定映射，不依赖线上索引先改字段

未完成：时间表周切换/顶部操作/等价 fixture；资讯空态与失败态；更多登录态；完整 PDF/Office/媒体附件。

## 深色模式

- Shell 根主题与主要页面已 token 化；深色六页冒烟通过，宿主实际截图验证。
- 修复颜色 helper 自递归导致的启动崩溃（`60b6aa7`）。
- 字体 0.85/1.3 冒烟；1.3 下 More 行高已改为 `heightIn`。
- 状态矩阵补：待办 3 条且 1/4 完成；资讯断网失败态；今日空/多课；时间表空课表；助手多消息。
- 用户接受当前矩阵；资讯空态、更多登录态、大量文本长会话为已知缺口。
- 浅色资讯页回归 0.000%。
- 未做上游深色逐页像素验收。

## Agent 已移植

- `agent/core`：10 个文件
- `agent/llm`：ChatCompletionModels、ReactPromptBuilder、LlmClient
- `agent/settings`：AI 加密设置
- `agent/model`：ChatSession、ChatMessageEntity、ChatMessageRecord、AgentResourceCard
- `agent/tools`：ReActTool、ReActToolRegistry、ToolHelper
- `agent/tools` 已实现 4 个：
  - `get_timetable`
  - `add_activity`
  - `search_empty_classroom`
  - `search_teacher`
- `ShellChatService`：宿主 ReAct 循环（最大 6 步）
- `AgentChatViewModel`：会话持久化、切换、删除
- `shell/agent/AgentChatScreen.kt`：上游布局、Markdown、AI 设置对话框
- `agent/data`：ChatDatabase、DAO、Store、Repository（Room）

## Agent 未完成

- 文本/PDF/DOCX/XLSX/PPTX 已接：系统文件选择器 → 解析文本 → 输入框；设备已验证四种格式。
- 图片/视频已接智谱多模态路由；图片已实际验证；视频 429。用户接受仅图片，视频记为 provider-429 已知缺口。
- 资源卡片已有渲染和点击代码；端到端互动仍待登录态/API Key 验证。
- `search_external_resource` 只接 HOA（`ResourceDataService`），未接 HITCS/薪火。

## API / 产物

- `TimetableService.getEventsDuringSync` 已加
- API AAR 已同步：
  - 模板 `9d9a196`
  - 社区 `2778baa`
- `hita-android-3.1.2` 通过宿主 shell 映射进入 Shell；HITA-L 仍走原生组合

## 下一步顺序

1. 按 M4 逐页对齐 6 个 Shell 页面（当前占位文案已移除）
2. 补助手附件上传与资源卡片交互
3. 重新采集六页截图并验收

## 运行时证据

- 设备：`emulator-5554`
- 注入 `preset_state.xml` 后启动，前台为 `PresetShellActivity`
- Room 库 `databases/hita_agent_chat` 已创建
- `chat_session` 实际存在一行：`新对话`
- 助手页坐标：标题 `[37,117][166,208]`，会话栏 `[64,320][575,383]`，欢迎卡 `[105,520][975,810]`
- 与上游同页截图差异：全屏 `8.05%`，去掉状态栏/导航区 `7.29%`
- 差异主因：附件按钮缺失、字体栅格化、底部 dock 背景
- 远程工具设备测试：`searchCoursesSync("高等数学")`、`braveSearchSync` 通过
- 后端健康检查：`https://api.hita.limpu.cn/health` 返回 `ok`
- 既有仪器测试仍有 2 个失败（`ExampleInstrumentedTest`、未登录成绩测试），与本次无关

## HITA Android 页面验收（2026-11-18 最新）

设备：`emulator-5554`，1080×2400，API 36。上游 `cn.limpu.hita` 与本项目同设备同分辨率。
上游已通过 API 登录建立会话；API 登录不走 MFA。

内容区裁剪 top 130 / bottom 310，tolerance 10：

| 页面 | 差异 | 结论 |
|---|---:|---|
| 待办 | 0.792% | 通过 |
| 今日 | 1.733% | 通过（单个已结束课程状态） |
| 助手 | 0.934% | 通过（空会话） |
| 资讯 | 11.49% | 未通过；卡片边缘/抗锯齿 |
| 更多 | 10.61% | 未通过；宿主登录态不同 |
| 时间表 | 13.53% | 未通过；fixture 第 1 周/测试课程 |

未覆盖：待办多条目/混合完成、今日无课/多课/跨节、时间表周切换、助手长会话/资源卡片、资讯空态/失败态、更多登录态、深色模式/字体缩放、header/dock 独立 ≤1% 阈值。

结论：Shell 结构已对齐 3 页并达到 2% 阈值；时间表已从节次网格改为时间轴网格；仍未整体一比一。

新页面（Shell 6 页、市场、插件容器、成绩/外观/培养方案）的逐项 UI 一致性审查见
`docs/plans/2026-11-19-new-page-ui-review.md`；
时间表/更多同步与缺失插件设计见 `docs/plans/2026-11-19-timetable-more-parity-design.md`。

## 0.7.2 时间表/更多与缺失插件（2026-11-19）

- 时间表右上角恢复「课表设置」，调用 `TimetableUiService.openStylePanel`；
- 更多新增：提交课程资料 PR、实用网址、公告、课程提醒、实时课程、使用统计；
- 课程提醒/实时课程/使用统计三个开关接入本地设置并在冷启动后保持；
- 新 UI 插件：`plugin-ui-announcement`（公告 API）、
  `plugin-ui-community`（深圳课程浏览原生课程池）；
- `plugin-ui-links` 移除了误混入的外观/字体控制；
- 深圳课程浏览已实现：课程池列表、学期/分页、课程名/教师关键词、详情、
  简介/中英文教学大纲附件、单课程选课；教师资料、完整选课任务、历史挂科率未完成。
- 使用统计：Room + WorkManager，默认关闭/首次询问，自托管 `/api/usage` 与
  `/api/market/stats` 已部署到 `hita.kajimi.cc`。
- 官方 `hita-community-plugins` registry 已同步（commit `6e9b386`），
  HITA-Android preset refs 含 announcement/community。
- 设备验证：课表设置面板、PR、实用网址、公告空态、深圳入口、三个开关持久化均通过。

## 像素复审（2026-11-19）

- 已关闭项复核确认不再优化：HITA-L 三页、Shell 待办/今日/助手内容、资讯 11% 残差、
  header/dock、视频 429、深色冒烟。
- 基准纠正：`com.stupidtree.hitax` 是 1.5.2；3.1.2 基准为 `cn.limpu.hita`。
- 更多页抽屉已按 3.1.2 重做：四条入口、账号两行、底部致谢；260dp 宽、72dp 头像、
  56dp 行高；头像 bounds 与上游完全一致。逐行差异 3.5%–9.0%，图标/字栅格残差；
  整面板因登录态不同不作通过结论。
- 成绩分析插件页：统一 Toolbar 已存在，但与上游“个人成绩明细”相比整体差异 9.941%；
  标题、刷新动作、未登录卡片结构仍不同，**未通过**。
- 无上游等价页面或缺少登录态/同数据的对象不记通过；详见
  `docs/plans/2026-11-19-pixel-audit.md`。
