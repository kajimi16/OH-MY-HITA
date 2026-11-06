# OH-MY-HITA

> **纯 vibe coding 项目**：功能、重构与测试主要由 AI 编码代理完成；发布前请自行审查代码与产物。

HITA 课表的插件化重构：微内核 + 插件，万物皆插件。

[App 下载（Releases）](https://github.com/kajimi16/OH-MY-HITA/releases/latest) ·
[国内镜像](https://ani2-ext.kajimi.cc/hita/releases/oh-my-hita-latest.apk)

当前发布：`0.6.0`（API 7）。包名 `io.github.kajimi16.ohmyhita`，与原版 HITA 可共存安装。

### API 7 能力

| 服务 | 能力 |
|---|---|
| `ReminderService` | 定时提醒、开机恢复、通知 |
| `WidgetContentProvider` | 插件桌面小组件 |
| `BackgroundTaskService` | 一次性/周期后台任务 |
| `LiveCourseService` | 直播课提醒与前台守护 |
| `ScoreService` | 学期成绩、GPA、排名 |
| `ExamService` | 考试列表 |
| `TeacherService` | 教师检索 |
| `TrainingPlanService` | 培养方案进度 |
| `EmptyClassroomService` | 教学楼与指定周空教室 |
| `ThemeController` | 主题、字体、圆角/间距 token |
| `TimetableAppearanceService` | 壁纸、透明度、模糊、遮罩 |

### 市场整合包

首次启动会直接列出整合包：

- `hita-l-1.5.3`：经典轻量组合
- `hita-android-3.1.2`：HITA_Android v3.1.2 复刻，19 个插件

市场卡片显示「使用中」，支持切换；切换会安装缺失插件，并停用旧组合独有的 UI/内置插件。
所有已安装 UI 插件也会出现在「功能中心 → 已安装插件」，不必从市场卡片进入。

当前复刻 UI 插件：作业、AI、资讯、提醒、网址、外观、学分、分享、成绩、培养方案、
对比、组合、直播、更新、通知。

### 已知限制

- 深圳账号已真实验证：成绩、空教室、培养方案、教师检索、门户通知。
- 深圳教务没有官方考试数据端点；考试页作为备忘录使用，不伪造考试时间。
- 主题与壁纸使用宿主 token 与模糊实现，未做 HITA_Android 的逐像素校准。
- 作业、好友课表等本地数据保存在插件自己的 SharedPreferences，不进入宿主数据库。

### 运行时插件（T1）

`kernel` 的 `PluginLoader` 支持从 dex 装载**纯逻辑插件**；另外宿主提供 Compose 容器，
可以把独立 APK 作为**界面插件**装载（不声明四大组件、不携带资源表）。

逻辑插件：

1. `PluginInstaller` 读取索引（构建期 `PLUGIN_INDEX_URL`，为空时读 `filesDir/market/index.json`）
2. 下载 → 校验 `sha256` → 复制到 `filesDir/runtime-plugins/<id>/plugin.dex`
3. `DexClassLoader` 装载 `entryClass` → `PluginHost.install(ctx)`
4. 应用启动时 `restore()` 自动恢复上次安装的插件

界面插件：

1. 索引的 `uiPlugins` 提供 APK 地址与 `sha256`
2. 校验后写入 `filesDir/ui-plugins/<id>/plugin.apk`，保留 `plugin.json` 元数据
3. 打开功能时 `RuntimeUiService` 按 `capabilities` 路由到 `PluginUiActivity`，
   在宿主的 Compose 容器里渲染；插件停用或文件损坏时自动回退内置页面

索引格式（线上索引在 `market` 分支维护）：

```json
{
  "plugins": [{"id":"com.example.hello","name":"示例逻辑插件","version":"0.1.0",
    "apiVersion":2,"url":"plugin.dex","sha256":"...","entryClass":"com.example.hello.HelloPlugin"}],
  "uiPlugins": [{"id":"com.stupidtree.hita.ui.resource","name":"课程资源","version":"0.4.8",
    "url":"https://ani2-ext.kajimi.cc/hita/market/course-resource-0.4.8.apk",
    "sha256":"...","description":"...","minHostApi":3}]
}
```

限制（Android 硬边界，不是实现取舍）：运行时逻辑插件不能声明四大组件、不能携带资源，
且与宿主同进程共享权限；界面插件同样不注册组件、不合并资源，只能通过宿主容器渲染。
需要真正独立进程/资源的插件仍走独立 APK（T2）。

示例逻辑插件在 `:sample-plugin`，产出 dex：

```bash
./gradlew :sample-plugin:buildPluginDex   # -> sample-plugin/build/plugin-dex/classes.dex
```

## 项目背景
项目最初来自哈尔滨工业大学（深圳）2018 级本科生大一年度立项，原名 HITSZ 助手，重构版改名为 HITA 课表。
该项目长期无人维护，本分支为接手维护的延续版本，同时附带了本研抓包逆向出来的api清单（由于更新时为非选课时间，部分选课api不能完全确认功能，同时部分api学校也没做完），便于后续更新。

## 开发与发布流程

```
分支 / PR ──push──▶ Dev build ──▶ Actions Artifacts（可直接安装的 APK）
   │
   └──合并到 master──▶ Release ──▶ 自动 patch+1、打 tag、发布 Release
```

- **分支开发**：每次 push 触发 `dev-build.yml`，产出 `oh-my-hita-dev-<sha>.apk`（artifact，保留 30 天）。
  下载：Actions 页面，或 `gh run download <run-id> -n oh-my-hita-dev-<sha>`。
- **发版**：合并进 `master` 即触发 `release.yml`：读取最近的 `v*` tag，patch +1，构建签名包并创建 Release；
  也可手动推 `v1.2.3` tag 指定版本。只改文档 / CI 配置的合并不会发版。
- **版本号**：`versionName` 来自 tag；`versionCode` 取构建时间戳（秒），dev 包与 release 包共用同一条单调序列，
  可互相覆盖安装。
- **签名**：CI 从仓库 Secrets 读取（`RELEASE_KEYSTORE_BASE64` / `RELEASE_KEYSTORE_PASSWORD` /
  `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`），仓库内不含任何密钥。


### 界面插件的安装

界面插件只能通过市场索引的 `uiPlugins` 安装/更新；客户端不提供本地选 APK 的入口。
调试使用 debug 广播通道（`scripts/dev-install-ui-plugin.sh`）。

### 本地联调（运行时插件）

- 课程资源数据契约 `ResourceDataService` 的端点是 `:plugin-resource` 的 BuildConfig。
  默认指向线上 HOA；本地联调可覆盖：`./gradlew :app:assembleDebug -PhoaBaseUrl=http://10.0.2.2:18001`，
  该属性仅用于开发，不要提交本地地址。
- 运行时界面插件从市场索引的 `uiPlugins` 下载；联调时可用本地 HTTP 索引：
  `-PpluginIndexUrl=http://10.0.2.2:8000/index.json`。


### 运行时界面插件的快速调试

- 一键调试：`./scripts/dev.sh status|reset|open|install-ui|install-dex|uninstall-dex|enable-dex|logs`
- 安装（debug 包）：`./scripts/dev-install-ui-plugin.sh plugin-ui-resource`
  构建 → `adb push` → `run-as` 落盘 → 广播安装 → 冷启动，单次约 3–5 秒，跳过市场下载。
- 对比截图：

  ```bash
  ./scripts/visual_diff.py builtin.png plugin.png --out diff.png \
      --crop-top 130 --crop-bottom 130 --tolerance 10
  ```

  输出差异像素数、比例与差异包围盒，并生成热力图。
- 直接打开插件页：`./scripts/dev-open-ui-plugin.sh com.stupidtree.hita.ui.resource CS`
  （debug 广播，免去从功能中心逐级点击）。
- 本地 mock：`-PhoaBaseUrl=http://10.0.2.2:18001`（仅联调，不要提交本地地址）。

搜索页复刻了内置的列表入场动画（400ms、每项延迟 20%、上移 20% + 淡入 + 105% 缩放）
与下拉刷新（PullRefreshIndicator，触发重新查询）。
当前课程资源搜索页与内置版（同一 mock 数据）截图差异约 **1.0%**：
几何、颜色、图标、字号、行高、tag 尺寸均已对齐（误差 ≤2px），
剩余差异集中在 Compose 与 `TextView` 的字体字形/抗锯齿，以及少量圆角边缘。

## 插件化架构（万物皆插件）

目标：把每位同学的改版从「fork 一个仓库」变成「选一组插件」。

| 模块 | 角色 |
|---|---|
| `:api` | 插件契约：`Plugin` / `PluginContext` / `ServiceKey` / `NavEntry` / `SearchTab` / `SyncCodec`，以及 API 7 的提醒、窗口、成绩、考试、教师、培养方案、空教室、主题等服务接口 |
| `:kernel` | 微内核：`PluginHost` 负责装载/卸载、注册表与回滚；不依赖 Android，可直接跑 JVM 单测 |
| `:app` | 宿主（shell）：`PluginHost` 装载/卸载插件并撤销其注册；`PluginCatalog` 是构建期插件清单 |
| `:model` | 共享数据模型：实体与 DTO，宿主 / 契约 / 插件三方共用 |
| `:plugin-hita` | 课表插件：Room 库、DAO、仓库、ICS、`TimetableService` / `TimetableUiService`、同步 codec，以及今日桌面小组件 |
| `:plugin-eas` | 教务插件：登录、成绩、考试、空教室、课表导入、成绩提醒，并提供 `ScoreService` / `ExamService` / `TrainingPlanService` / `EmptyClassroomService` |
| `:plugin-resource` | 课程资源插件：HOA 课程资料、README 渲染、投稿、教师检索（`ResourceUiService`） |
| `:plugin-user` | 个人主页插件：资料页与编辑页（`UserUiService`） |
| `:theta` | 社区插件：贡献 `CommunityService`、搜索页两个 tab、抽屉入口 |
| `:component` `:style` `:user` `:sync` | 待迁移的能力层（P2/P3 目标） |

社区插件不在本仓库：例如番剧界面插件位于
[`kajimi16/hita-community-plugins`](https://github.com/kajimi16/hita-community-plugins)，
通过市场索引安装，不随宿主发版。

插件发布也由社区仓库完成：作者运行 `Publish Plugin` 工作流，CI 构建产物、计算 sha256、
写入 `dist/` 与中央索引 `registry/index.json`；宿主默认读取该索引，`market` 分支保留为可替换源。

装配点只有一个 —— `app/src/main/assets/plugins.json`：

```json
{
  "enabled": ["com.stupidtree.hita.theta", "com.stupidtree.hitax.timetable", "..."],
  "plugins": [{"id": "...", "class": "...", "needsApplication": true}]
}
```

`PluginCatalog` 反射实例化这些类，宿主不 import 插件实现（`app` 只以 `runtimeOnly` 依赖插件模块）。
从 `enabled` 里删掉一项 → 该功能不再被装配，宿主代码一行不用改（已用空壳构建+启动验证）。
要真正从 APK 里移除，还需删掉 `app/build.gradle` 的 `runtimeOnly` 与 `settings.gradle` 的 include。

已实现的贡献点：

- `ServiceKey<T>` —— 服务注册/查找（`CommunityService` 即由 θ 插件提供）
- `NavEntry` —— 抽屉导航项（构建期用资源标题，运行时插件用字符串标题）
- `SearchTab` / `ViewSearchTab` —— 搜索结果页 tab；运行时 dex 插件用 View 形态，避免 Fragment 恢复失败
- `SyncCodec` —— 云同步的「一张表」，宿主不再 `when(key)` 硬编码数据表
- `TimetableUiService` —— 课表界面入口与小组件刷新
- `ResourceUiService` —— 课程资源 / 教师界面入口
- `UserUiService` —— 个人主页入口（θ 与宿主都只传 userId）
- `HostUiService` —— 宿主的跨插件能力（导入、弹窗、跳转），由宿主实现

约定：

1. 插件只依赖 `:api`，禁止依赖 `:app` 或其它插件模块。
2. 插件里的一切注册（服务、导航项、监听器）都通过 `PluginContext` 完成，内核按相反顺序回滚；
   单个插件装载失败不影响其它插件。
3. 跨插件只传 id 与 DTO，不共享 `@Entity`；每个插件一个独立的 Room 库。
4. `api` 的任何破坏性改动都必须递增 `HITA_API_VERSION`。

### 内置插件的卸载

插件市场的「内置插件」区列出五个构建期插件（θ 社区、课表、教务、课程资源、个人主页），用户可以卸载或重新启用：

- **卸载**：`PluginHost.uninstall(id)` 热撤销服务 / tab / 导航项 / 同步表，同时用 `PackageManager`
  禁用该插件声明的 Activity、桌面小组件 Receiver 与 RemoteViewsService，并持久化禁用标记；
  下次启动不再装配。
- **清数据**：卸载对话框可选「卸载并清除数据」。删除动作延迟到界面重建之后执行，
  若进程中途退出则由下次 `Application.onCreate` 在装配前兜底（`PluginPurge`）。
  清理范围由各插件实现 `Plugin.purgeData()`：课表库与样式偏好、教务凭据与成绩提醒、插件私有目录。
- **恢复**：重新启用会重新实例化并安装插件、恢复组件、清空待清理标记。

限制（Android 平台的硬边界，不是实现取舍）：

1. 代码仍在同一个 APK 里，卸载**不会减小安装体积**；
2. 底部导航仍是 4 个固定槽位：被卸载插件对应的位置显示占位页（说明 + 一键跳转市场），
   不做动态缩容；
3. 已放在桌面的小组件实例需要手动删除，否则会显示无法加载；
4. 清除应用数据会丢失插件禁用标记，等同于重装应用；启动时会按标记收敛一次组件开关状态，
   避免出现「插件已装配、组件仍禁用」。

要做到系统级「真卸载、体积真减少」，必须把插件拆成独立 APK（见 T2 协议），
跨进程契约与独立发版成本见上一节讨论。

### 新增一个插件

1. 新建 Gradle 模块，`implementation project(':api')`。
2. 实现 `Plugin`，在 `install(ctx)` 里 `ctx.provide(...)` 或 `ctx.nav.register(...)`。
3. 在 `PluginCatalog.plugins()` 里加一行。

## 应用简介
这是面向哈尔滨工业大学（深圳）学生的工具类 APP（非官方）。

### 当前主要功能
- 课表与日程：导入课表、按周查看、课程详情
- 教务服务：成绩查询、学分绩与排名、空教室查询
- 选课助手：定时发包抢课（按设定时间发 5 次）
- 课程资源：应用内搜索课程资料与 README、支持追加型投稿
- 教师搜索：优先使用课程资源数据，同时提供教师主页检索入口
- 考试：暂无官方接口，提供考试备忘录

## 数据与版权说明
- 课程与课表数据来自教务系统，本应用不额外采集或上传。
- 课程资料来源 HOA（校内民间开源组织），欢迎同学参与贡献。官网：hoa.moe
- 如有问题请联系：2720649216@qq.com

## 用到第三方开源库
- 加载效果按钮：[LoadingButtonAndroid](https://github.com/leandroBorgesFerreira/LoadingButtonAndroid)
- 显示多行的 CollapsingToolbarLayout：[multiline-collapsingtoolbar](https://github.com/opacapp/multiline-collapsingtoolbar)
- θ 社区上传图片压缩：[Luban](https://github.com/Curzibn/Luban)
- 今日页下拉交互：[PullLoadXiaochengxu](https://github.com/LucianZhang/PullLoadXiaochengxu)

## License

[MIT](LICENSE) © Stupid Tree

### 下载镜像

`ani2-ext.kajimi.cc` 上的 APK 由服务器每小时通过 GitHub API 自动同步
（systemd timer，见 `deploy/mirror/`），保留全部历史版本：

- 最新：`https://ani2-ext.kajimi.cc/hita/releases/oh-my-hita-latest.apk`
- 指定版本：`https://ani2-ext.kajimi.cc/hita/releases/oh-my-hita-<version>.apk`
- 元数据：`https://ani2-ext.kajimi.cc/hita/releases/index.json`

