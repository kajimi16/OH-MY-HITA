# HITA Android 3.3.1 与版本化整合包设计

日期：2026-11-23

## 1. 目标

- 保留 `hita-android-3.1.2` 原样，新增 `hita-android-3.3.1`。
- 两个版本共存；切换不清空对方数据。
- 首启与市场按“发行版 → 版本号”选择整合包。
- 插件市场允许选择插件版本安装。
- 允许新增 T2/T1 插件承载 3.3.x 新功能，不把旧 Room/登录/组件塞回宿主。

## 2. 上游差异

HIT-A/HITA_Android `v3.1.2...v3.3.1`：154 commits，300 files，+15,834 / -957。

按能力分组：

- 首页/今日：3.3.0 首页重做，进行中课程、明日课程、考试/待办摘要。
- 待办：Grader 作业与本地待办统一，GeniusTA 导入、完成状态按账号隔离。
- 校园服务：校园卡、静音仓、图书馆研习间、实验室预约（深圳）。
- 课表：课程版本对比、教务来源变化、误删恢复、历史。
- 提醒/日历：下课与考试提醒、系统日历同步。
- 成绩：成绩分布、本地旧数据提示。
- 缓存：3.3.1 教务/校园查询先显示上次成功结果，失败显示旧数据时间并可重试。
- AI：写操作前确认，新增确认协议与工具结果模型。

结论：3.3.1 是功能增量，不能靠改 3.1.2 preset 的版本字符串伪装；需要独立 preset、独立 shell 与新增插件。

## 3. 共存模型

- 新增 preset id `hita-android-3.3.1`，`family = "hita-android"`，`shell = "hita-android-3.3.1"`。
- `hita-android-3.1.2` 增加同一 `family`，其余字段不动。
- `PresetStateStore` 仍只保存 active id；两个 id 各自完整，切换不共享可变安装状态。
- `PresetShellRouter` 把两个 shell id 都路由到 `PresetShellActivity`；shell 内 tabs 和插件来自所选 preset。
- 数据迁移只在用户显式选择时执行；两个 preset 共用宿主 Room/TimetableService，不建第二套数据库。

## 4. 版本化索引

旧字段保持兼容：顶层 `version/url/sha256/...` 仍表示 latest。

新增可选字段：

```json
"family": "hita-android",
"versions": [
  {"version":"3.1.2","url":"...","sha256":"...","size":0,"minHostApi":11},
  {"version":"3.3.1","url":"...","sha256":"...","size":0,"minHostApi":11}
]
```

- `MarketPreset`、`MarketPlugin`、`UiMarketPlugin` 都增加 `family` 与 `versions`。
- 版本条目只覆盖 artifact 字段；description/tags/repo 等展示字段沿用顶层。
- 解析规则：未声明 `versions` 时退化为单版本（顶层）；声明后以条目为准。
- 选择版本后构造该版本的视图对象，沿用现有 installer/校验逻辑，不复制安装器。

## 5. 选择界面

### 首启

- 按 `family` 聚合整合包；卡片显示发行版名、说明、当前选中版本。
- 卡片内版本 chip/下拉：默认 latest；选择后只改变即将应用的版本。
- `推荐` 给 3.1.2 或 3.3.1 的当前稳定线，`使用中` 显示 active id 精确匹配。

### 市场

- 整合包详情增加版本下拉；切换后主按钮文案为「应用 vX.Y.Z」。
- 插件详情增加版本下拉；选择版本后按钮为「安装 vX.Y.Z / 更新 / 降级」。
- 已安装版本与 latest 不同时显示 `已安装 vX` + `可更新到 vY`。
- 版本选择不持久化为全局默认；只作用于本次动作。

## 6. 新插件边界

- 校园服务：新增 T1 `CampusService` + T2 `ui-campus`，页面不直连教务/校园网。
- 待办/Grader：`ui-homework` 新版本；GeniusTA 凭据与同步走新 T1，不写入宿主账号表。
- 课表版本/历史：`timetable-core` 增数据接口，`ui-timetable` 增 UI。
- 日历/提醒：宿主服务或 T1，组件/通知由宿主承接。
- 3.3.1 preset 只引用已发布版本；未发布功能插件不得标 required。

## 7. 分阶段

1. 版本模型与解析器：`family/versions`、selected variant、单元测试；旧索引单版本兼容。
2. 首启/市场版本选择 UI：整合包按 family 聚合，插件详情版本下拉。
3. `hita-android-3.3.1` preset + shell 映射 + 3.1.2 共存；先引用现有已发布插件。
4. 新功能插件：campus、todo/Grader、timetable history、calendar/reminders；逐个独立版本发布并加入 3.3.1。
5. 双版本矩阵与回退验证。

## 8. 验收

- 3.1.2 与 3.3.1 都在市场/首启可见，版本号可选择，切换后 active id 正确。
- 同一插件切换版本安装，`installed.json` 记录所选版本，市场显示已安装版本。
- 3.1.2 行为不变；3.3.1 可独立切换，不互相停用。
- 新插件构建/安装/打开不崩溃；未发布插件不出现在 required 列表。
- 旧索引（无 versions）解析为单版本，现有 preset 全部可读。

## 9. 被否决方案

- 把 3.1.2 preset 的 version 改成 3.3.1：数据/插件没有变化，属于伪造版本。
- 用同一个 preset id 原地覆盖：无法共存与回滚。
- 所有插件都做成宿主内置：破坏插件边界与独立发布。
- 版本选择持久化为全局默认：会让一次临时选择影响后续所有安装。

## 10. 当前实现状态

- 已实现：`family`/`versions` 索引字段、旧索引单版本回退、插件按版本构造、整合包按 family 聚合、首启与市场版本选择、3.1.2/3.3.1 共存与 shell 切换。
- 已实现：`hita-android-3.3.1` preset 先复用 3.1.2 的 25 个已发布插件与 tabs，作为新功能插件的接入点。
- 已实现：3.3.x 的 AI 写操作确认（`add_activity`、`submit_review`）；3.1.2 及更早 preset 保持直接执行。
- 未实现：3.3.x 的校园服务、待办/Grader、课表历史、日历提醒等新功能；它们必须先发布对应 T1/T2 插件，再替换 3.3.1 preset 的引用。

| 3.3.1 能力 | 状态 | 落点 |
|---|---|---|
| 整合包/插件版本选择 | 已接入 | `family` / `versions` + 首启/市场选择器 |
| 3.1.2 与 3.3.1 共存 | 已接入 | 独立 preset id + shell 切换 |
| AI 写操作确认 | 已接入 | `add_activity` / `submit_review`，仅 3.3.x |
| 校园卡/静音仓/研习间/实验室 | 数据层 + UI 已接入（3.3.x 可选插件）；多人静音仓已接联系人/读者核验；登录/网络未实机验证 | plugin-campus + plugin-campus-ui |
| 待办/Grader/GeniusTA | 数据层 + UI 已接入（3.3.x 可选插件）；Grader 已接应用内 CAS WebView，Cookie 粘贴保留兜底；视觉 SSIM 0.846 | plugin-todo + plugin-todo-ui |
| 课表版本历史/恢复 | 已接入（3.3.x 限定） | 宿主壳版本记录/对比/恢复；仅 CLASS 事件、按 timetableId 隔离、保留 5 版 |
| 日历同步/下课提醒 | 下课前提醒、课表同步系统日历、考试提醒已接入（3.3.x 限定） | 宿主 Worker/CalendarContract/ExamService/设置行 |

### 课表版本页视觉/行为证据

- 原版基线：HITA_Android `v3.3.1` APK，sumi 主题，1080×2400，`upstream-331-history.png`。
- 移植版：同一模拟器、同一尺寸与主题，`host-history-sumi-final.png`。
- SSIM：标题+首张版本卡 `0.8976`；整页 `0.8783`。未达 0.98，差异集中在状态栏高度、标题/正文字体度量与首卡纵向偏移；已知未解决。
- 行为：导入 9 条 ICS 课程生成 v1；修改一条 CLASS 课程后生成 v2；对比 v2→v1 显示“详情 1 节”；恢复 v1 后生成 v3，当前版本标记“已恢复”。
- 隔离：版本只记录 `type=CLASS && timetableId=当前课表`；恢复删除列表同样按 timetableId 过滤，其他课表不受影响。

### 下课前提醒行为证据

- 设置行只在 3.3.x 显示，3.1.2 实测隐藏。
- 开启后 `course_reminder.xml` 写入 `end_enabled=true`；通知权限被正确请求。
- 插入一条 `CLASS` 事件，结束时间设为 10 分钟后；立即运行 Worker 后系统通知栏出现：
  - 标题：`⏰ 即将下课`
  - 正文：`下课提醒验收 @ A101 将在 5分钟后结束`
  - 去重键：`end:<eventId>`
- 开始提醒与结束提醒使用 `start:`/`end:` 前缀，同一课程两条通知互不吞掉；旧的无前缀已发送 ID 仍兼容。
- 原版使用独立设置页，宿主当前为 More 内联开关；该入口没有做像素级对照，已记录为外观差异。

### 课表同步系统日历行为证据

- 模拟器创建本地可写日历（`_id=1`）。
- 首次同步写入 11 条课程事件；再次同步后仍为 11 条，未产生重复。
- 每条事件 `DESCRIPTION` 包含 `hita:<timetableId>:<eventId>`，用于更新与清理。
- 权限拒绝、无可写日历、写入失败分别返回显式结果；3.1.2 不显示入口。
- 该入口为宿主内联行，原版无同位置组件，未做像素级对照。

### 视觉比对规范（新增页面必须先过）

- 基线：HITA_Android `v3.3.1` APK，固定 1080×2400、中文、默认浅色主题、动画禁用，同一模拟器。
- 采集：每页分别截上游与移植版；裁除状态栏、系统导航栏与动态时间区域，保留页面内容区。
- 指标：`skimage.metrics.structural_similarity`，目标 `SSIM ≥ 0.98`。
- 流程：单页最多 3 轮修正；仍低于 0.98 时保留 todo open，记录分数、差异图和阻塞原因，不标 done。
- 实时数据页同时报告 raw 与排除实时数字区域后的 masked 分数；两者都是证据，不自行放宽 0.98。
- 无同页基线或形态不同（如静音仓上游 WebView、宿主登录卡）记录 `N/A` 与原因，保持 open；`N/A` 不等于通过。
- 2026-11-24 用户明确：先核对上游数据与字体实现，三轮后放宽。已完成上游 `HitaComposeTheme`/`HitaTheme.tokens`/字体核对与多轮结构对齐；按用户授权，校园页保留当前分数并记录差异，不再继续追 0.98。
- 新页面清单：校园卡、静音仓列表/预约、图书馆研习间、实验中心、待办合并列表、Grader 登录、GeniusTA 登录、考试提醒入口。

### 待办/校园页面证据（2026-11-23）

- Todo UI 已运行：合并列表、账号状态、GeniusTA 账号密码登录、Grader Cookie 粘贴、完成切换。
- 待办视觉：上游 v3.3.1 `upstream-331-todo-final.png` vs 移植版 `host-todo-ui.png`，内容区 SSIM `0.8459`，未达 0.98；停留在 todo open。
- Campus UI 已运行：四标签、未登录提示、WebView 登录、日期/范围/预约/取消入口；重复的未登录卡片与上游布局差异大，未做 SSIM 达标声明。
- Campus 插件 12 个测试类/144 个用例、Todo parser 测试均通过；真实门户登录与网络未验证。
- 校园四页已通过上游同包 instrumentation 采集未登录基线（`app/build/upstream-campus-baseline/`）：校园卡 `0.8596`、静音仓 `0.7997`、图书馆 `0.8984`、实验中心 `0.9054`。均未达 0.98，保留 open。
- 校园卡（静态未登录页）做了 3 轮修正：单页模式 + 白色主题 + 登录按钮移出卡片 + 文案对齐；最终 `0.9224`，仍低于 0.98。差异集中在工具栏动作项、字体度量与卡片高度；按 3 轮上限停止。
- 图书馆页为实时数据：raw SSIM `0.9221`，排除实时数字区域后的 masked `0.8410`；未达 0.98，保留 open，等待用户决定是否接受或改用固定数据基线。工具栏标题已对齐为“大学城图书馆”。
- 实验中心为静态未登录页：1 轮结构对齐（去掉页内标题、登录卡居中、窄按钮、文案对齐）后 `0.9269`；整体上移降到 `0.9197` 已回退。
- 静音仓基线是门户 WebView 登录页，宿主是登录卡：形态不同，SSIM `N/A`；去掉重复登录卡/日期/预约段只作为行为改进。
- 考试提醒：政策测试证明仅 `hita-android-3.3.x` 启用；3.1.2 启动后 jobscheduler 无 `exam_reminder` 任务，More 行隐藏。
- 考试通知已实机验证：20 分钟后开考 + 30 分钟档 fixture，`dumpsys notification` 出现 `考试提醒` / `考试提醒验收 @ A101，30 分钟后开考`，channel `exam_reminder`。
- 考试数据：真实 `ExamService` 已读取（内容脱敏）；到期通知未在到期日实机观察，通知投递由 fixture 验证。
- 校园基线：已通过上游同包 instrumentation 采集未登录四页；真实登录态未验证。
- 真实考试链路：使用一次性测试账号登录成功，`ExamService` 返回考试列表（内容已脱敏）。排程单测使用合成 payload 验证 24h 档触发。
- 到期通知未在到期日实机观察；通知投递已由 fixture 验证。会话与凭据 prefs 已清除，账号未写入仓库。2026-11-24 用户接受“真实数据读取 + 合成 payload 排程单测 + fixture 通知”作为该项验收。









