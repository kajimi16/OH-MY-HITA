# 自定义能力盘点（2026-11-20）

> 范围：当前 0.8.0 本地开发树（API 9）。只列“当前不能由用户、整合包或插件自定义”的能力，
> 以及可扩展点；不把已支持的能力写成缺口。

## 1. 结论

| 层级 | 当前不可自定义 |
|---|---|
| 主题 | 自定义配色、圆角、间距、玻璃透明度、字体家族/字重 |
| 动效 | 自定义时长/缓动/逐页动画；多数宿主动画未接 MotionController |
| 音效 | 自定义音效资源、音量/开关 UI、通知声音 |
| 布局 | HITA-Android 固定 6 tab；整合包 `tabs` 不生效；tab 不可排序/改名/换图标 |
| 插件容器 | 宿主 Toolbar 固定；插件不能贡献标题动作/返回行为/转场 |
| 插件依赖 | UI 插件不能声明插件/整合包依赖；无版本范围/可选依赖/循环诊断 |
| 整合包 | 插件不能声明基于哪个 preset/shell/布局版本 |
| 通知 | 渠道重要性/振动/声音固定；更多页课程提醒文案固定 15 分钟 |
| 其他 | 应用图标/名称、启动壳、全局密度/字体、卡片布局等 |

## 2. 主题与视觉

### 已支持

- `ThemeController`：夜间模式、4 个配色 preset、7 个主题风格、字体缩放 0.85–1.30。
- `ThemeTokens`：主题派生的圆角、间距、玻璃透明度（只读）。
- `TimetableAppearanceService`：卡片透明度、模糊、遮罩、壁纸导入/清除。

### 不可自定义

- 颜色：不能输入自定义 seed/色板，只能选固定 preset。
- 组件形状：圆角/间距/玻璃由主题风格硬编码，无用户覆盖。
- 字体：只能缩放，不能选字体家族、字重、字形。
- 卡片/页面布局：今日卡、资讯卡、更多页分组的尺寸/间距/结构固定。
- 应用图标与名称：无运行时自定义。

证据：

- `api/src/main/java/com/stupidtree/hita/api/ThemeController.kt`
- `app/src/main/java/com/stupidtree/hitax/HApplication.kt`（`tokens()` 固定映射）
- `plugin-ui-appearance/.../AppearanceUiPlugin.kt`

## 3. 动效

### 已支持

- API 9 `MotionController`：standard/expressive/reduced，读取系统动画缩放。
- Shell tab 切换已使用 `MotionTokens`。

### 不可自定义

- 只能选 profile，不能注册自定义 duration/easing 曲线。
- 市场进度窗口仍是市场页自己的固定动画。
- 插件不能替换宿主页面转场，只能在自身 Compose 内容里动画。
- 没有逐页面/逐组件的独立覆盖 UI。

### 已覆盖（2026-11-20）

- tab 切换、抽屉、今日课程卡展开、时间表切周已使用 MotionTokens。

证据：

- `app/src/main/java/com/stupidtree/hitax/shell/PresetShellActivity.kt`
- `app/src/main/java/com/stupidtree/hitax/motion/MotionControllerImpl.kt`

## 4. 音效与触感

### 已支持

- API 9 `FeedbackService`：TAB/CLICK/SUCCESS/ERROR/NOTICE 语义音效；
  LIGHT/MEDIUM/HEAVY/SUCCESS/WARNING/ERROR 触感；音量/开关设置。
- 宿主统一静音与系统触感开关；默认关闭。

### 不可自定义

- 不能提供自定义音频文件；只能使用宿主内置 ToneGenerator 音色。
- 通知渠道的声音/振动 pattern 固定；课程提醒、直播课、成绩提醒各自硬编码。
- 插件不能绕过宿主统一静音策略。

### 已补充（2026-11-20）

- More → 设置新增“界面音效”“触感反馈”和音量滑杆；
- 设置写入宿主的 `FeedbackService`，默认关闭。

证据：

- `api/src/main/java/com/stupidtree/hita/api/FeedbackService.kt`
- `app/src/main/java/com/stupidtree/hitax/feedback/FeedbackServiceImpl.kt`
- `app/src/main/java/com/stupidtree/hitax/data/work/CourseReminderWorker.kt`

## 5. Shell/布局

### 已支持

- 整合包索引有 `tabs`、`shell`、`plugins`。
- HITA-L 会读取 `PresetLayoutStore.tabs`。
- 运行时插件可声明 `mainTabs`。
- **2026-11-20 已实现**：HITA-Android Shell 也读取 `PresetLayoutStore.tabs`；
  支持排序、标题、隐藏、内置图标；`visible=false` 可隐藏（`required=true` 除外）；
  未知 tab 显示占位页而不是静默删除。

### 仍不可自定义

- 底栏编辑已实现拖拽排序、改名、内置图标切换、隐藏；仍缺自定义图片图标。
- 未知 UI 插件 tab 目前是占位页，没有嵌入插件内容。
- tab 宽度、胶囊高度、指示器、抽屉宽度等尺寸固定。
- 更多页分组、入口顺序、图标固定；插件不能贡献更多页行。
- 插件容器 `PluginUiActivity` 固定 56dp Toolbar，标题取插件名；
  插件不能提供标题、右侧动作或自定义返回/转场。

证据：

- `app/src/main/java/com/stupidtree/hitax/shell/PresetShellActivity.kt`
  （`ShellTab` 列表、tab 尺寸）
- `app/src/main/java/com/stupidtree/hitax/ui/main/MainActivity.kt`
  （HITA-L 读取 `PresetLayoutStore`）
- `app/src/main/java/com/stupidtree/hitax/uiplugin/PluginUiActivity.kt`

## 6. 插件依赖与整合包绑定

### 已支持

- 构建期/dex 插件：`PluginDescriptor.requires` 服务名，缺失时等待。
- 整合包：`PresetPluginRef.required` 控制组合必选；`minHostApi`。

### 不可自定义

- UI 插件没有 `requires`；缺服务只能拿到 `null`。
- 没有插件 id 依赖、版本范围、可选依赖、循环检测。
- 没有 `requiresPreset` / `supportedPresets` / `layoutVersion` / `targetShell`。
- 插件不能声明“基于 HITA-Android 3.1.2 界面开发”，宿主不能提示“未验证”。
- 切换整合包不会对已安装 UI 插件做 UI 合约检查。

证据：

- `kernel/.../PluginDescriptor.kt`
- `kernel/.../PluginHost.kt`
- `app/.../market/MarketPlugin.kt`
- `app/.../uiplugin/UiPluginManager.kt`

## 7. 通知与提醒

- 课程提醒的更多页开关文案固定“上课前15分钟”，但
  `CourseReminderStore.DEFAULT_MINUTES = 10`，提醒插件默认滑杆 15；
  文案、默认值、实际调度可能不一致。
- 提前分钟数只能通过提醒插件改，更多页没有滑杆/输入框。
- 直播课触发窗口固定；通知渠道重要性、振动、声音固定。
- 用户不能为不同通知类型选声音/振动。

## 8. 建议优先级

1. **P0 布局契约**：HITA-Android Shell 已消费 `PresetLayoutStore.tabs`；
   编辑 UI 已实现；下一步补自定义图片图标、未知 UI 插件 tab 的内容嵌入。
2. **P1 UI 依赖/整合包绑定**：`requires.plugins`、`uiContract.targetPreset`；
   软兼容提示，不自动卸载。
3. **P1 Motion 剩余项**：市场进度、自定义 profile/曲线、逐页覆盖。
4. **P2 反馈设置**：更多页增加音效/触感开关与音量；通知声音独立设置。
5. **P2 视觉 token**：自定义圆角/间距/字体家族，或至少开放 preset 级别的覆盖。
6. **P3 插件容器**：允许插件声明标题、Toolbar actions、转场类型。

## 9. 明确已支持、不是缺口

- 自定义市场索引源 URL。
- 主题夜间/配色/风格/字体缩放。
- 时间表壁纸、透明度、模糊、遮罩。
- 插件声明 capabilities/mainTabs。
- 整合包必选插件缺失阻断。
