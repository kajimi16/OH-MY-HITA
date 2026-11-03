# 课表插件化迁移设计

## 目标

宿主（`:app`）不再认识任何课表概念，课表变成 `:plugin-hita`。

定义性验收：`PluginCatalog` 去掉课表插件、`app/build.gradle` 去掉 `:plugin-hita` 依赖后，
构建通过、应用能启动、只是没有课表功能。

## 现状（S0 之后）

| 层 | 位置 | 说明 |
|---|---|---|
| Room 库 / 实体 / DAO | `:plugin-hita` | `AppDatabase`、`EventItem`/`TermSubject`/`Timetable`、三个 DAO |
| 仓库 | `:plugin-hita` | `TimetableRepository`、`SubjectRepository`、ICS 导入、Gson 工具已迁入；EAS 相关仓库仍在宿主 |
| UI / Worker / Widget | `:app` | 216 个 .kt 中的大部分，直接 import 实体 |
| 服务契约 | `:api` | `TimetableService`（35 个方法），宿主只通过它访问课表 |
| 共享模型 | `:model` | 实体与 DTO，宿主/契约/插件三方共享；不进 `:api` 是为了保持契约只有接口 |

## 切片

### S0（已完成）解耦数据层的隐式 UI 依赖

迁移前数据层反向依赖了 UI，必须先拆：

| 依赖 | 处理 |
|---|---|
| `Timetable` → `TimetableFragment.Companion.WEEK_MILLS` | 常量移到数据层 `TimeMath.kt`，Fragment 保留同义常量 |
| `EventItem`/`TimeInDay` → `utils.TimeTools` | 纯计算抽成 `TimeMath`，`TimeTools` 改为委托，避免两份实现漂移 |
| `TermSubject` → `utils.ColorTools` | `ColorTools` 移入 `:component`（纯 Kotlin，无 Android 依赖） |
| `EventItemDao` → `ui.timetable.detail.TeacherInfo` | `TeacherInfo` 移入数据模型包 |

验收：构建、`:kernel` 与 `:app` 单测、模拟器启动并打开课表页、无 Room 异常。

### S1 定义 `TimetableService`（已完成）

- 契约在 `:api`，模型在 `:model`：`:api` 只放接口，实体放共享模型模块。
- 覆盖宿主全部读写需求：19 个读接口、14 个写接口、3 个 ICS 接口。
- 插件在 `install(ctx)` 里 `ctx.provide(TimetableService.KEY, ...)`。
- 宿主通过 `HitaServices.timetable(context)` 取用；缺插件时直接抛错，不静默降级。

验收：宿主 19 个文件、31 处调用全部改为服务调用；`grep` 确认宿主不再引用仓库类。

### S2 仓库搬入插件（数据/仓库部分已完成）

- ✅ `TimetableRepository` / `SubjectRepository` / ICS 导入 / `GsonBuilderUtil` 已移入 `:plugin-hita`。
- ⬜ 插件内实现 `TimetableService` 并在 `install(ctx)` 里 `provide`（下一步）。
- 插件内实现 `TimetableService`，并在 `install(ctx)` 里 `provide`。
- 宿主侧用 grep/AUnit 校验：`app` 不再 import `data.model.timetable.*`。

验收：新增一个读接口时，只需要改插件与服务契约。

### S3 UI 搬入插件

- 课表 Fragment / Activity 移入插件，通过 `NavEntry` 与宿主预留的容器暴露。
- 宿主只保留通用壳（单 Activity、Fragment 槽、主题 token）。
- 这一步才真正达成「删掉 HITA 仍是空壳可运行」。

验收：删掉 `:plugin-hita` 依赖与装配行后构建通过、应用启动、首页为空状态。

### S4 系统集成去特化

- `CourseReminderWorker`、桌面小组件等由插件注册，宿主提供通用注册点。

## 风险

| 风险 | 说明 |
|---|---|
| Room 迁移归属 | 数据库在插件内，schema 版本与迁移脚本必须跟插件走；宿主不再持有备份/升级逻辑 |
| 资源依赖 | T0（同 APK 编译）阶段插件资源可与宿主合并；将来若改运行时加载，UI 必须改为宿主 token 或 assetPath 注入 |
| 反射耦合 | `theta` 的 `ActivityTools.startUserActivity` 里用 `Class.forName("com.stupidtree.hitax.ui.profile.ProfileActivity")`，属于隐式跨模块引用，S3 前要清掉 |
| 单仓库多模块同包名 | 迁移期保留 `com.stupidtree.hitax.data.*` 包名以减少改动；S3 结束后应统一到 `com.stupidtree.hita.plugin.timetable` |

## 回滚

每个切片独立可回滚：S0/S1 是纯增量；S2 之后若出问题，恢复 `app/build.gradle` 对插件的依赖并删掉 `PluginCatalog` 中的一行即可回到上一状态。
