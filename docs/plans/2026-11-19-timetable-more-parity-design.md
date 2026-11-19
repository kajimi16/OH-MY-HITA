# 时间表 / 更多 UI 同步与缺失插件设计（2026-11-19）

> 状态：设计先行，分阶段实现  
> 基准：HITA_L 1.5.3 与 HITA_Android 3.1.2  
> 输入：`docs/plans/2026-11-19-new-page-ui-review.md`

## 1. 目标

把 HITA-Android Shell 的「时间表」「更多」补齐到 3.1.2 的结构，并为缺失能力建立可安装的
UI 插件，而不是继续在 Shell 里堆硬编码页面。

具体缺失：

1. 时间表右上角缺「课表设置」，当前误放「切换深浅色」；
2. 更多缺「提交课程资料 PR」「实用网址」「公告」；
3. 更多缺「课程提醒」「实时课程」「使用统计」开关；
4. 「深圳课程浏览」因 `com.stupidtree.hita.ui.community` 不存在被隐藏；
5. 插件容器页缺统一返回/TopAppBar，成绩分析页与上游 chrome 不一致（关联项）。

## 2. 能力归属

| 功能 | 归属 | 理由 |
|---|---|---|
| 课表设置 | 宿主 Shell → `TimetableUiService.openStylePanel` | 面板已存在课表插件内，无需新页面 |
| 提交课程资料 PR | 宿主入口 → 现有 `ResourceUiService.openSearch(submit=true)` | 已有 `CourseContributionActivity` 与资源插件 |
| 实用网址 | 新入口 → 现有 `plugin-ui-links` | 插件已存在，只缺入口 |
| 公告 | 新 UI 插件 `plugin-ui-announcement` | 现有 notice 插件是校园通知，语义不同；公告需要独立的版本/服务公告页 |
| 课程提醒开关 | 宿主 Shell → `CourseReminderStore` + `CourseReminderScheduler` | 调度器是宿主能力，插件不能声明 Worker |
| 实时课程开关 | 宿主 Shell → `LiveCourseSettings` | HITA-L 已有同名开关；HITA-Android 用同一设置存储 |
| 使用统计开关 | 宿主 Shell → 新增 `UsageStatsStore` | 当前没有遥测后端；先持久化开关并明确“本版本不上传” |
| 深圳课程浏览 | 新 UI 插件 `plugin-ui-community` | 目录页是独立业务；已接 EAS 课程池，选课/附件/挂科率后续补 |
| 插件页返回/TopAppBar | 宿主 `PluginUiActivity` 或插件自绘 | 先在插件页自绘统一 header，避免一次性改所有旧插件 |

## 3. 时间表设计

### 3.1 顶部动作

参考 HITA_Android 3.1.2：右上角为「分享与好友」和「课表设置」。

改造：

- 保留分享按钮；
- 把「切换深浅色」替换为「课表设置」；
- 点击调用 `TimetableUiService.openStylePanel(activity)`；
- 深浅色仍可从「更多 → 应用设置 → 外观与字体」进入，不丢失。

### 3.2 验收

- 时间表右上角只出现分享、课表设置；
- 点击课表设置打开 `FragmentTimetablePanel`（或插件对应 style panel）；
- 返回后时间表状态不丢失；
- 分享仍能拉出系统分享面板。

## 4. 更多页设计

参考 HITA_Android 3.1.2 的分组：

```text
更多
  课程资源
    查询资源
    提交课程资料 PR
    实用网址
  设置
    课程提醒      [Switch]
    实时课程      [Switch]
    使用统计      [Switch]
    公告          >
```

### 4.1 入口

- 提交 PR：`HitaServices.resourceUi(context)?.openSearch(context, null, submit = true)`；
  服务缺失时 Toast，不静默。
- 实用网址：`openPlugin("com.stupidtree.hita.ui.links")`；未安装时 Toast。
- 公告：`openPlugin("com.stupidtree.hita.ui.announcement")`；未安装时 Toast。
- 深圳课程浏览：恢复入口，`openPlugin("com.stupidtree.hita.ui.community")`；
  插件未安装时显示“插件未安装”，不再静默隐藏。

### 4.2 开关

- 课程提醒：`CourseReminderStore.setEnabled` + `CourseReminderScheduler.autoSchedule`；
  开启时请求 `POST_NOTIFICATIONS`；拒绝则保持关闭并提示。
- 实时课程：`LiveCourseSettings.setEnabled`；状态实时刷新。
- 使用统计：`UsageStatsStore`（SharedPreferences，默认 `false`，首次启动询问）；
  开启后走 Room 队列 + WorkManager 上传，可清除。

### 4.3 验收

- 每个新行点击都有明确结果（页面、系统面板或 Toast）；
- 三个开关切换后杀进程重启仍保持；
- 课程提醒开关开启后 WorkManager 唯一任务存在；
- 实时课程关闭后 `LiveCourseSettings.isEnabled == false`；
- 更多页顺序与 3.1.2 对齐。

## 5. 新插件设计

### 5.1 `plugin-ui-announcement`

- id：`com.stupidtree.hita.ui.announcement`
- 页面：公告列表；顶部返回、标题、刷新；
- 数据：HITA 公告 API（`https://api.hita.limpu.cn/api/notices`）；
  无公告时显示空态，失败显示重试；
- 空态/失败态/重试齐全；
- 不引入新依赖（`HttpURLConnection` + `org.json`）。

### 5.2 `plugin-ui-community`

- id：`com.stupidtree.hita.ui.community`
- 页面：深圳课程浏览；
- 首版：原生课程池列表（`CourseCatalogService` → EAS `getAvailableCourses`）；
  未登录显示「连接 Web 教务」，已登录按学期分页展示课程；
- 已实现：课程列表、关键词检索、课程详情、简介/中英文教学大纲附件、单课程选课；
- 明确限制：历史挂科率受教务隐私限制；完整选课任务、教师资料页仍未移植；
- 插件不声明 Activity/Service，仍由 `PluginUiActivity` 承载。

### 5.3 插件页 chrome

新插件自绘统一 header（返回 + 标题 + 动作），返回调用 `host.close()`。
旧插件（成绩/外观/培养方案）后续统一迁移到同一 header 组件。

## 6. 分阶段实施

### Phase 1（本次，已完成设备验证）

1. 时间表课表设置；
2. 更多：PR、实用网址、公告入口；
3. 更多：课程提醒/实时课程/使用统计开关；
4. 恢复深圳课程浏览入口并接入新插件；
5. 新插件模块与构建：
   - `plugin-ui-announcement`：公告列表（HITA 公告 API）；
   - `plugin-ui-community`：深圳课程浏览（原生课程池）；
6. 清理 `plugin-ui-links` 中误混入的外观/字体控制；
7. 两个新插件 APK 加入 `market/index.json` 与 `market/`（本地/市场索引样例）；
8. `CourseCatalogService` 原生课程池契约与 EAS 实现。

### Phase 2

1. 深圳课程浏览补齐选课任务/历史挂科率/教师资料；
2. 公告接入已读状态。

## 7. 设备验证（2026-11-19）

- 时间表右上角只保留「分享课表」「课表设置」；点击课表设置打开样式面板
  （主题、起始时间、背景虚线、科目颜色、背景渐变、左侧节次、自动导入）。
- 更多新增：提交课程资料 PR、实用网址、公告、课程提醒、实时课程、使用统计。
  - PR 打开资源搜索的“选择课程并追加内容”；
  - 实用网址打开清理后的链接页（不再混入外观/字体控制）；
  - 公告打开新插件；后端当前返回空列表时显示“暂无公告”；
  - 课程提醒请求通知权限后写入 `course_reminder`，开关重启保持；
  - 实时课程写入 `live_courses`，使用统计写入 `usage_stats`，重启保持。
- 深圳课程浏览打开原生课程池：未登录显示连接 Web 教务；已登录按学期分页展示课程；
  详情/附件/关键词/单课程选课已实现；选课任务/历史挂科率/教师资料仍待 Phase 2。
- 新插件模块：
  - `plugin-ui-announcement`（0.1.0）
  - `plugin-ui-community`（0.1.0）

构建/校验：`:app:assembleDebug`、`:app:testDebugUnitTest`、`:app:lintDebug`、
两个新插件的 `lintDebug`、`:plugin-ui-links:lintDebug` 全部通过。

## 8. 风险与已知边界

- 自有索引已在 `hita.kajimi.cc` 部署，HITA-Android refs 含新插件；上游官方
  `hita-community-plugins` 索引已同步（6e9b386）。
- 使用统计后端已部署到 `hita.kajimi.cc`；实际人数需要真实客户端事件累积。
- 历史挂科率受教务隐私限制，不能合法生成其他教学班统计。
- 完整选课任务、教师资料页尚未移植；附件真实内容未在登录态设备验证。


## 9. 实现状态审计（2026-11-19）

| 设计项 | 状态 | 证据/缺口 |
|---|---|---|
| 时间表「课表设置」 | 完成 | 设备打开样式面板；分享按钮保留 |
| 更多「提交课程资料 PR」 | 完成 | 打开“选择课程并追加内容” |
| 更多「实用网址」 | 完成 | 打开清理后的 links 插件 |
| 更多「公告」入口 | 完成 | 打开 announcement 插件；内置“OH-MY-HITA 开发中”公告，后端空列表仍可见 |
| 课程提醒开关 | 完成 | 权限、pref、WorkManager 周期任务均已验证 |
| 实时课程开关 | 完成 | `live_courses` 持久化 |
| 使用统计开关 | 完成 | 自托管 `/api/usage` 已部署；Room/WorkManager/白名单/清除已验证 |
| 深圳课程浏览 | 部分完成 | 列表/关键词/详情/附件/选课已完成；教师资料、完整选课任务、历史挂科率未完成 |
| 新插件模块/本地市场索引 | 完成 | 两个 0.1.0 APK、sha256、settings.gradle |
| 新插件随整合包自动安装 | 完成（自有索引） | `hita.kajimi.cc` 索引已带 HITA-Android refs；上游官方索引未改 |
| 公告已读状态 | 未完成 | Phase 2 |
| 旧插件统一 TopAppBar | 完成 | 所有运行时插件使用 PluginUiActivity Toolbar，重复标题已移除 |
| 更多页与 3.1.2 完全同构 | 部分完成 | 新增行/抽屉完成；仍保留宿主独有的应用设置入口；上游像素未全量复核 |

结论：**Phase 1 主路径完成；Phase 2 与整合包发布链路未完成**。
