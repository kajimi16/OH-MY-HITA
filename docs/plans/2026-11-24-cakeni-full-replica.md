# cakeni/CourseSchedule 全量复刻设计（Compose / XL）

## 目标

新增宿主侧完整 Shell `replica-cakeni`，用 Compose 重写 cakeni/CourseSchedule 的完整界面：

- preset id：`hita-replica-cakeni`
- shell id：`hita-replica-cakeni`
- 4 个主 tab：课表 / 待办 / 导入 / 设置
- 入口：`PresetShellRouter` → `CakeniReplicaActivity`
- 数据：`TimetableService`、`TodoService`、`HostUiService`

## 许可边界

cakeni/CourseSchedule 为 MIT。允许参考和改写其代码，但必须保留 MIT 版权与许可；本项目仍为 MIT。

不复用：
- `app/src/main/res/raw/course_duck.riv`（鸭子 Rive 资源，权利状态不清）
- `academic_school_directory_NOTICE.txt` 中的学校目录数据
- 启动图、截图、品牌图形

本实现为 Compose 重写，不直接搬运 cakeni 的 View 代码。设计参考：大日期、周摘要、5 天网格、课程卡、玻璃底栏、日期数字滚动、周切换。

## 范围（XL）

### 课表
- 大日期：年/月/日、星期、第 N 周。
- 周切换：左右按钮、回到本周、横向滑动动画。
- 周摘要：当前周日期范围、课程数。
- 5 天网格（周末可开关）：节次时间轴、课程卡、重叠分栏。
- 日期数字滚动：日/月变化时逐位滚动。
- 课程卡：课名、教室、教师、开始/结束时间。
- 课程详情 BottomSheet：当前周、星期、节次、时间、教师、教室。
- 空周状态。

### 待办
- 接 `TodoService`：全部/今天/7 天内筛选。
- 已完成切换；来源登录状态提示。
- 空状态、加载状态。
- 不新增待办（HITA 的 TodoService 没有创建接口）。

### 导入
- 页面顶部是「教务账号」卡：未登录显示「登录教务」主按钮，导入按钮禁用；已登录显示账号、主按钮「导入课表」和「重新登录」。
- 「登录教务」启动 `ShellEasWebLoginActivity`，在官方统一身份认证页面完成账号与短信验证码；应用只接收会话 cookie，不接触密码/验证码。
- 登录成功后 `EasAuthService.loginWithWebSession` 建立 T1 会话；cakeni Activity 重建并刷新账号状态。
- 「导入课表」调用 `EasService.openImportTimetable(activity, autoImport = true)`，不重写解析器。
- 原生 `EasAuthService.login(username,password)` 没有验证码参数，暂不作为登录入口。
- ICS/文件导入继续走宿主导入入口；插件市场入口保留。

### 设置
- 深色/浅色/跟随系统。
- 显示周末、显示非本周课程、显示节次时间。
- 关于、插件市场、版本信息。

## 视觉

- 背景：很浅的冷灰蓝。
- 卡片：白色，16dp 圆角，轻阴影。
- 主色：蓝色；课程色：珊瑚、紫、青、蓝、粉、绿。
- 底栏：`Haze` 背景模糊（API 31+），低版本半透明+边框降级；4 个 tab，选中蓝色胶囊。
- 大日期 40sp+，标题半粗。

## 数据契约

`CakeniCourse`：
- id/name/teacher/place/dayIndex/from/to/startSlot/endSlot/color/type
- `startSlot` 来自 `EventItem.fromNumber`
- `endSlot = fromNumber + lastNumber - 1`（`lastNumber` 是持续节数）
- 不伪造课程周次范围；详情只显示当前查看周。

`CakeniWeek`：
- week、weekStart、days(5 或 7)、courseCount。

`CakeniSource`：
- 读取 `getRecentTimetable()` 与 `getEventsDuringSync()`。
- 纯函数 `buildWeek`，空服务返回空模型，不崩溃。

## 模块

```text
replica-cakeni/
  CakeniReplicaActivity (app)
  CakeniRoot.kt
  CakeniTheme.kt
  CakeniModels.kt
  CakeniSource.kt
  schedule/ScheduleScreen.kt
  schedule/DateHeader.kt
  schedule/CourseDetailSheet.kt
  todo/TodoScreen.kt
  importdata/ImportScreen.kt
  settings/SettingsScreen.kt
```

## 验收

1. 市场切换到 `hita-replica-cakeni`，冷启动进入新 Shell。
2. 课表显示真实课程，5 天网格；周末开关生效。
3. 左右切周、回到本周；日期数字有滚动动画。
4. 课程详情显示当前周、节次、时间、教师、教室。
5. 待办页显示 TodoService 数据；完成切换调用服务。
6. 导入页能打开宿主教务导入。
7. 设置页切换深色/浅色；显示周末/非本周/节次时间生效。
8. `:replica-cakeni:testDebugUnitTest`、`:app:testDebugUnitTest`、`:app:assembleDebug`、`:replica-cakeni:lintDebug` 通过。
9. 模拟器截图验证 4 个 tab。

## 非目标

- 不复制鸭子 Rive、学校目录、品牌图。
- 不做 cakeni 的 AI 助手页面。
- 不做完整课程编辑器；新增课程走宿主 `openAddEvent`。
- 不做通知/提醒的 cakeni 实现，复用宿主。

## 风险

- cakeni 是 View 体系；Compose 重写需要自行实现日期滚动、玻璃底栏、周切换。
- 课程周次范围不可直接从 `EventItem` 得到；详情只显示当前周。
- 玻璃模糊在低版本 Android 上不可用；用半透明+边框降级。


## 实施状态（2026-11-24）

已完成：

- `replica-cakeni` 模块、`CakeniReplicaActivity`、`PresetShellRouter` 分支。
- 4 tab：课表 / 待办 / 导入 / 设置。
- 课表：大日期、逐位滚动、周切换、周摘要、5/7 天网格、重叠分栏、课程详情、FAB。
- 待办：TodoService 合并列表、今天/7天/全部筛选、完成切换、登录态/空状态。
- 导入：教务导入、插件市场入口。
- 设置：跟随系统/浅色/深色、显示周末、显示非本周、显示节次时间、关于/市场。
- 本地 preset `hita-replica-cakeni` 只在根 `market/index.json` 与 debug fallback；release fallback 不包含。

验证：

- 模拟器实际切换 preset 后进入 `CakeniReplicaActivity`。
- 4 个 tab、周切换、课程详情、深色、显示周末均截图验证。
- `:replica-cakeni:testDebugUnitTest`、`:app:testDebugUnitTest`、`:replica-cakeni:lintDebug`、`:app:assembleDebug` 通过。

已知限制：

- 课程详情不显示完整周次范围（EventItem 无该字段）。
- 7 天模式课程卡较窄；5 天模式是主推布局。
- 不复制 cakeni 鸭子 Rive、学校目录、品牌图。

## 数据与发布边界

- UI 层不做事件去重。重复事件若来自导入/数据层,应在 `plugin-hita` 的 `TimetableEventDedupe` 修复;`CakeniModels` 只按服务返回的原始事件渲染。
- 同一时段的不同课程会分栏;不按课名/节次合并,避免误删真实冲突。
- `market/index.json` 是本地/构建输入索引,不打进 APK;debug 使用 `app/src/debug/assets/market/index.json`;release fallback `app/src/main/assets/market/index.json` 不包含预览 preset。公开 registry 也未发布。

## 重复事件说明

- `plugin-hita` 已有 `TimetableEventDedupe`，键包含 name/place/teacher/from/to/fromNumber/lastNumber。
- 复刻 UI 不做二次去重。当前 fixture 仍会出现两张同名卡，来源(导入/时间戳差异)尚未确认;不在 UI 层掩盖。

## 短信验证码无头 WebView 方案评估

目标：隐藏 WebView 加载统一认证页，用外部 Compose 输入框填写账号/密码/短信码，再通过 JS 注入完成点击与提交，最后捕获 cookie 建立 T1 会话。

调研结论：**暂不可行，未实现**。

证据（模拟器，未输入凭据）：
- `https://trust.hitsz.edu.cn/portal/...` 页面 DOM 返回 `inputs=[]`、`buttons=[1 个空 A]`、`iframes=[]`、`title="Loading..."`。
- 页面脚本反复探测 `localhost.sangfor.com.cn:54631/54642/54653` 与 `127.0.0.1:54630/54641/54652`，均失败；疑似 aTrust 客户端检测未通过。
- WebView 无法进入账号表单，外部输入无 DOM 目标可填，无头套壳无法完成短信步骤。

设计保留（未来若页面暴露标准表单）：
- 可见/离屏 WebView + 外部输入字段。
- 只注入 `input`/`change`/`click`,不读取密码；不写日志、不落盘。
- 通过 DOM 状态机识别“账号 → 短信 → 成功”，每一步只在用户显式点击后提交。
- 成功后 `EasAuthService.loginWithWebSession(cookies, base)`。
- 必须有本地 mock 页面测试，再对真实门户验证；禁止自动点击发送验证码/提交。

### 无头桥接的安全约束（若未来实现）

- 只做 fill-only relay：外部输入后只填字段/点“发送验证码”，不自动提交；页面提交只由用户显式点击触发。
- 密码与验证码通过 `JSONObject.quote` 作为参数传入 JS，不拼字符串、不写日志、不落盘。
- DOM 探测只读取结构字段（tag/type/name/id/placeholder），禁止读取或记录 `value`。
- 选择器必须用真实页面实时确认；先可见 WebView 验证，再考虑离屏/无头。
- 不复制其他项目的注入脚本；脚本按本站点 DOM 编写，并有本地 mock 页面测试。
