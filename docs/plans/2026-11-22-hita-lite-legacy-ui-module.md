# HITA_Lite legacy UI 模块设计

## 0. 目标形态确认（2026-11-22）

用户确认采用**构建期合并的 UI 模块**，不是运行时市场 APK 插件：

- `:legacy-hitalite` 作为 Android Library 合并进宿主 APK。
- 保留 Lite 的 XML、Activity、Fragment、View 与资源前缀，以像素还原为优先。
- 不满足“运行时独立 APK 插件”的能力边界；这是用户明确选择的目标形态。
- 不新增第二套登录、EAS、Room 或 Widget receiver。

## 1. 决策

不用运行时 UI 插件 APK，也不整包热插 HITA_Lite 单体。

采用宿主侧 legacy UI 模块：

```text
:app
  └─ :legacy-hitalite（Android library，旧 XML/资源/Activity）
        └─ 只保留 UI 与 ViewModel
              └─ 通过适配器调用宿主服务
```

新增 preset `hita-lite-2.0`，映射到独立 shell `hita-lite-2.0`。

不移植：

- 旧登录 `EASWebLoginActivity` / `EASource`
- 旧 Room `AppDatabase` / DAO / Repository
- 旧 Widget receiver `TodayWidget` / `TodayWidgetSlim` / `ListWidgetService`

## 2. 为什么必须重新命名空间

HITA_Lite 与宿主/`plugin-hita` 存在同名包和类：

```text
com.stupidtree.hitax.ui.main.MainActivity
com.stupidtree.hitax.ui.main.NavigationFragment
com.stupidtree.hitax.ui.main.timetable.TimetableFragment
com.stupidtree.hitax.ui.main.timeline.FragmentTimeLine
```

如果直接复制到 library，会与 app/plugin-hita 产生重复类。

模块规则：

- Kotlin/Java 包统一改为 `com.stupidtree.hitax.legacy.lite.*`。
- 所有 `import com.stupidtree.hitax.ui...` 改为 `com.stupidtree.hitax.legacy.lite.ui...`。
- 所有 `R.xxx` 解析到 legacy 模块自己的 `R`。
- 资源文件名加 `lite_` 前缀，避免与宿主资源合并冲突。
- 布局/Drawable/String/Menu 的引用在搬入模块时同步改名。
- 旧 `data.*`、`component.*`、`style.*` 不复制；改用宿主服务适配器。

这是机械迁移，不是手工重写；需要 codemod + 编译校验。

## 3. 模块边界

```text
legacy-hitalite/
  AndroidManifest.xml        # 只声明 Lite shell 与子 Activity，不声明 Widget receiver
  java/.../legacy/lite/
    ui/main/                 # 旧 MainActivity -> HitaLiteActivity
    ui/main/timeline/        # 今日
    ui/main/timetable/       # 时间表/管理/详情
    ui/main/navigation/      # 功能中心
    ui/eas/                  # 成绩/考试/空教室/导入（UI only）
    ui/search/ ui/teacher/   # 搜索/教师
    ui/news/ ui/profile/     # 新闻/个人页
    ui/about/ ui/widgets/    # 关于/协议/通用控件
    data/                    # 只保留 UI model 与 adapter，不含 Room/Retrofit
  res/                       # 旧资源，文件名 lite_ 前缀
```

依赖只允许：

```text
:api :model :component :style :user
```

禁止依赖 `:plugin-hita` 的 Repository/DAO，禁止新增 Room 数据库。

## 4. 数据流与接口

### 4.1 今日/时间表

```text
Lite Fragment/ViewModel
  → LiteTimetablePort
      → TimetableService（宿主）
```

`LiteTimetablePort` 是模块内接口，默认实现包宿主 `TimetableService`：

- 课表列表、当前课表、事件区间、今日事件
- 保存/删除事件仍走宿主 service
- UI model 转换成 Lite 旧 `EventItem` 的只读映射

契约：

- 前置条件：宿主 `TimetableService` 已注册。
- 后置条件：UI 只读宿主数据，不创建第二份数据库。
- 失败处理：service 缺失时显示空态，不崩溃。

### 4.2 教务子页

```text
Lite Score/Exam/Classroom/Import UI
  → LiteEasPort
      → EasService / EasAuthService（宿主）
```

- 登录入口调用宿主 `EasAuthService` 或宿主 Web 登录 Activity。
- 不复制 Lite 的账号密码表单和 cookie 存储。
- 成绩/考试/空教室/导入只复用 Lite 的列表、筛选、空态、对话框。
- 导入结果写入宿主课表 service。

### 4.3 用户/搜索/新闻

- 个人页：`UserService` + 宿主头像/昵称。
- 搜索/教师：宿主 `ResourceUiService` 或现有搜索入口；Lite 只保留结果列表外观。
- 新闻：宿主新闻/公告数据源；Lite 只保留详情排版。

### 4.4 主题

- 不引入 Lite `ThemeTools` 单例。
- 旧 XML 主题属性映射到宿主 `ThemeController` token。
- 只保留 Lite 的浅色/深色两态，不扩展 CUTE 主题。

## 5. 宿主 seam

最小改动：

1. `settings.gradle` 新增 `:legacy-hitalite`。
2. `app/build.gradle` 依赖 `implementation project(':legacy-hitalite')`。
3. `PresetStateStore.shellForPreset()` 增加：

```kotlin
"hita-lite-2.0" -> "hita-lite-2.0"
```

4. `PresetStateStore`/`MainActivity.redirectToActiveShell()` 将 shell id 映射到
   `HitaLiteActivity`，仅在该 preset 下生效。
5. `market/index.json` 等三份索引增加 `hita-lite-2.0`，`shell` 显式声明。
6. 不修改现有三个 preset 的页面分支、tabs 或主题行为。

## 6. 验收 fixture

- 设备：API 36 模拟器，1080×2400。
- 原版：HITA_Lite v2.1 APK（固定 sha256）。
- 主题：浅色、中文、动画禁用。
- 空数据：首次启动，无课表、未登录。
- 固定数据：同一份 ICS（固定课程/科目/事件），导入到原版与宿主；登录页只验证未登录态。
- 截图：每页双状态，裁除状态栏；SSIM ≥ 0.98。
- 小组件：同一 Launcher、同一尺寸，裁到组件边界比较。
- 入口矩阵：三页、课表管理/详情、成绩、考试、空教室、导入、搜索、教师、新闻、个人页、抽屉、关于、协议/对话框。

## 7. 实施阶段

1. 建 `:legacy-hitalite` 模块，空 Shell + 资源前缀规则。
2. codemod 迁移 UI/资源，去掉旧 data/DB/widget receiver。
3. 接入 `LiteTimetablePort` / `LiteEasPort`，完成今日/时间表。
4. 功能中心 + 课表管理/详情。
5. 成绩/考试/空教室/导入。
6. 搜索/教师/新闻/个人页/抽屉/关于/对话框。
7. preset、shell 映射、索引三份 + registry。
8. fixture、截图、SSIM、回归测试。

## 8. 风险

- 资源前缀/包重命名会产生大量引用错误；以编译错误驱动 codemod。
- 宿主 style 与 Lite 旧 style 版本不同，主题层需要适配，不复制旧 style 单例。
- SSIM 0.98 对字体/系统渲染敏感；必须固定设备、主题、字体缩放和动画。
- 原版 APK 当前下载不稳定；baseline 不可用时按 Stop condition 停止并上报。

## Widget exception (2026-11-22)

User explicitly authorized new Lite Widget receivers for the widget slice,
overriding the earlier no-new-receiver boundary. Widgets still must read host
services and must not add a Lite Room/EAS database.
