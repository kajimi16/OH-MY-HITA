# OH-MY-HITA

HITA 课表的插件化重构：微内核 + 插件，万物皆插件。

[App 下载（Releases）](https://github.com/kajimi16/OH-MY-HITA/releases/latest)

当前版本：`0.0.2`。包名 `io.github.kajimi16.ohmyhita`，与原版 HITA 可共存安装。

### 运行时插件（T1）

`kernel` 的 `PluginLoader` 支持从 dex 装载**纯逻辑插件**：

1. `PluginInstaller` 读取索引（构建期 `PLUGIN_INDEX_URL`，为空时读 `filesDir/market/index.json`）
2. 下载 → 校验 `sha256` → 复制到 `filesDir/runtime-plugins/<id>/plugin.dex`
3. `DexClassLoader` 装载 `entryClass` → `PluginHost.install(ctx)`
4. 应用启动时 `restore()` 自动恢复上次安装的插件

索引格式：

```json
{"plugins":[{"id":"com.example.hello","name":"示例插件","version":"0.1.0",
"apiVersion":2,"url":"plugin.dex","sha256":"...","entryClass":"com.example.hello.HelloPlugin"}]}
```

限制（Android 硬边界，不是实现取舍）：运行时插件不能声明四大组件、不能携带资源，
且与宿主同进程共享权限。因此市场只上架逻辑插件；需要界面的功能走独立 APK（T2）。

示例插件在 `:sample-plugin`，产出 dex：

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

## 插件化架构（万物皆插件）

目标：把每位同学的改版从「fork 一个仓库」变成「选一组插件」。

| 模块 | 角色 |
|---|---|
| `:api` | 插件契约：`Plugin` / `PluginContext` / `ServiceKey` / `NavEntry` / `SearchTab` / `SyncCodec`。只有接口，不含实体类 |
| `:kernel` | 微内核：`PluginHost` 负责装载/卸载、注册表与回滚；不依赖 Android，可直接跑 JVM 单测 |
| `:app` | 宿主（shell）：`PluginHost` 装载/卸载插件并撤销其注册；`PluginCatalog` 是构建期插件清单 |
| `:model` | 共享数据模型：实体与 DTO，宿主 / 契约 / 插件三方共用 |
| `:plugin-hita` | 课表插件：Room 库、DAO、仓库、ICS、`TimetableService` 实现与同步 codec |
| `:theta` | 第一个插件样例：只依赖 `:api`，贡献 `CommunityService`、搜索页两个 tab、抽屉入口 |
| `:component` `:style` `:user` `:sync` | 待迁移的能力层（P2/P3 目标） |

装配点只有一个 —— `app/src/main/java/com/stupidtree/hitax/kernel/PluginCatalog.kt`：

```kotlin
object PluginCatalog {
    fun plugins(): List<Plugin> = listOf(
        ThetaPlugin(),
    )
}
```

删掉一行 → 该功能不再被装配，宿主代码一行不用改（已用模拟器验证）。
注意这只是「不装配」：模块仍会被编译打包；要真正从 APK 里移除，需同时删掉 `app/build.gradle`
的模块依赖和 `settings.gradle` 的 include。

已实现的贡献点：

- `ServiceKey<T>` —— 服务注册/查找（`CommunityService` 即由 θ 插件提供）
- `NavEntry` —— 抽屉导航项
- `SearchTab` —— 搜索结果页 tab，按 `SearchTabIds` 语义 id 与宿主对齐
- `SyncCodec` —— 云同步的「一张表」，宿主不再 `when(key)` 硬编码数据表

约定：

1. 插件只依赖 `:api`，禁止依赖 `:app` 或其它插件模块。
2. 插件里的一切注册（服务、导航项、监听器）都通过 `PluginContext` 完成，内核按相反顺序回滚；
   单个插件装载失败不影响其它插件。
3. 跨插件只传 id 与 DTO，不共享 `@Entity`；每个插件一个独立的 Room 库。
4. `api` 的任何破坏性改动都必须递增 `HITA_API_VERSION`。

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
