# OH-MY-HITA

HITA 课表的插件化重构：微内核 + 插件，万物皆插件。

[App 下载（Releases）](https://github.com/LiPu-jpg/HITA_L/releases/latest)

## 项目背景
项目最初来自哈尔滨工业大学（深圳）2018 级本科生大一年度立项，原名 HITSZ 助手，重构版改名为 HITA 课表。
该项目长期无人维护，本分支为接手维护的延续版本，同时附带了本研抓包逆向出来的api清单（由于更新时为非选课时间，部分选课api不能完全确认功能，同时部分api学校也没做完），便于后续更新。

## 插件化架构（万物皆插件）

目标：把每位同学的改版从「fork 一个仓库」变成「选一组插件」。

| 模块 | 角色 |
|---|---|
| `:api` | 插件契约：`Plugin` / `PluginContext` / `ServiceKey` / `NavEntry`。只有接口，不含实体类 |
| `:app` | 宿主（shell）：`PluginHost` 装载/卸载插件并撤销其注册；`PluginCatalog` 是构建期插件清单 |
| `:theta` | 第一个插件样例：只依赖 `:api`，往宿主导航注册「θ社区」入口 |
| `:component` `:style` `:user` `:sync` | 待迁移的能力层（P2/P3 目标） |

装配点只有一个 —— `app/src/main/java/com/stupidtree/hitax/kernel/PluginCatalog.kt`：

```kotlin
object PluginCatalog {
    fun plugins(): List<Plugin> = listOf(
        ThetaPlugin(),
    )
}
```

删掉一行 → 该功能不参与本次构建，宿主代码一行不用改（已用真机/模拟器验证）。

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
