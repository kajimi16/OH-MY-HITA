# 自定义能力 API 扩展方案

> 日期：2026-11-20  
> 依据：`2026-11-20-customization-gaps.md`；当前 API 9 本地实现。  
> 本文只给 API 与运行时方案，不实现 0.8.0 的后续代码。

## 1. 目标

让下面这些对象从“宿主写死”变成“可声明、可覆盖、可诊断”：

1. HITA-Android Shell 的 tab 布局；
2. 主题颜色/圆角/间距/字体；
3. 宿主动效曲线与逐组件动画；
4. UI 音效/触感与通知声音；
5. 插件容器标题/动作/转场；
6. 插件之间、插件与整合包之间的依赖。

同时保留三条硬边界：

- 系统无障碍/静音/系统动画缩放永远优先；
- 用户显式选择永远高于整合包和插件推荐；
- Android 不能安全运行时修改的项不做伪 API。

## 2. 统一优先级

```text
系统设置（无障碍/静音/动画缩放）
  > 用户显式覆盖
  > 整合包推荐
  > 插件默认/推荐
  > 宿主默认
```

插件不能直接覆盖用户设置；整合包不能绕过用户安全开关；任何非法值回退到宿主默认并记录诊断。

## 3. Shell / Tab 布局

### 3.1 当前缺口（2026-11-20 部分已实现）

- HITA-Android Shell 现在读取 `PresetLayoutStore.tabs`，不再固定 6 项；
  已支持排序、标题、隐藏与内置图标。
- 用户编辑 UI 已实现拖拽/改名/内置图标/隐藏；未知 UI 插件 tab 已通过 PluginUiHost 内嵌。

### 3.2 建议数据模型

```kotlin
data class ShellTabSpec(
    val id: String,                 // 稳定 id，如 hita.timeline / plugin id
    val title: String,
    val icon: IconRef,              // Builtin / APK asset / plugin mainTab
    val order: Int,
    val visible: Boolean = true,
    val required: Boolean = false,  // 安全/产品必需 tab
)

data class ShellLayoutSpec(
    val id: String,
    val tabs: List<ShellTabSpec>,
    val dock: DockSpec = DockSpec(),
)

data class DockSpec(
    val maxTabs: Int = 6,
    val heightDp: Float? = null,
    val cornerRadiusDp: Float? = null,
)
```

### 3.3 解析与合并

1. 内置/预设提供基础 `ShellLayoutSpec`；
2. UI 插件通过现有 `MainTabSpec` 贡献 tab；
3. 用户布局（`PresetLayoutStore.tabs`）覆盖顺序/标题/可见性；
4. 合并后校验：
   - `required` tab 不可隐藏；
   - tab 数、标题长度、图标尺寸、点击区域有上限；
   - 未知 id 保留为 `degraded`，不静默丢弃；
5. HITA-Android Shell 与 HITA-L 都消费同一份布局。

### 3.4 用户编辑

后续如获明确需求，再新增“编辑底栏”页面：

- 拖拽排序、隐藏/显示、重命名；
- 恢复预设默认；
- 不能隐藏宿主安全入口（如“更多”）；
- 编辑结果写入 `PresetLayoutStore`，不修改整合包索引。

### 3.5 插件容器

`PluginUiHost` 增加可选 chrome：

```kotlin
interface PluginChrome {
    fun setTitle(title: String)
    fun addAction(action: PluginActionSpec)
    fun setTransition(transition: PluginTransition)
}

data class PluginActionSpec(
    val id: String,
    val title: String,
    val iconAsset: String? = null,
    val enabled: Boolean = true,
)
```

实现边界：

- Toolbar 仍由宿主绘制，避免插件自带 Activity/资源表；
- 动作回调通过 `PluginChrome` 注册，随 `onDestroy` 撤销；
- 返回行为默认 `close()`，插件可声明 `confirmOnBack`；
- 不允许插件替换宿主 Window/系统栏。

## 4. 主题与视觉 token

### 4.1 现状

`ThemeTokens` 只读，固定由主题风格映射；没有自定义色板/圆角/字体家族。

### 4.2 建议扩展

保留 `ThemeController` 现有方法，增加：

```kotlin
data class ThemeOverride(
    val seedColorArgb: Int? = null,
    val palette: Map<String, Int> = emptyMap(),
    val cornerRadiusDp: Float? = null,
    val spacingDp: Float? = null,
    val glassAlpha: Float? = null,
    val fontFamilyAsset: String? = null,
    val fontScale: Float? = null,
)

interface ThemeController {
    fun override(): ThemeOverride
    fun applyOverride(override: ThemeOverride): Boolean
    fun clearOverride()
    fun observe(): LiveData<ThemeState>
}
```

约束：

- `seedColor`/palette 做对比度校验；不达标回退；
- 圆角/间距/玻璃夹在安全范围；
- `fontFamilyAsset` 仅允许宿主导入/校验过的字体，不直接读任意路径；
- `userOverride` 存 `theme` 偏好；整合包/插件只能写“推荐值”，需要用户确认；
- `tokens()` 仍返回最终生效值，插件只读，避免插件间互相覆盖。

### 4.3 视觉测试

- 浅色/深色、7 个主题风格；
- 自定义 seed 的正文/背景对比度；
- 字体 0.85/1.30；
- 插件读取 tokens 与宿主截图一致。

## 5. 动效

### 5.1 现状（2026-11-20 更新）

API 9 的 `MotionController` 已接入 tab 切换、抽屉、今日卡、时间表切周和市场进度窗。

### 5.2 建议角色化

```kotlin
enum class MotionRole {
    TAB_SWITCH, DRAWER_OPEN, CARD_EXPAND,
    WEEK_SWITCH, PROGRESS_SHOW, SHELL_ENTER,
}

data class CubicBezier(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

data class MotionSpec(
    val durationMs: Int,
    val delayMs: Int = 0,
    val easing: CubicBezier? = null,
)

data class MotionProfile(
    val id: String,
    val specs: Map<MotionRole, MotionSpec>,
)

interface MotionController {
    fun registerProfile(profile: MotionProfile): Boolean
    fun applyProfile(profileId: String): Boolean
    fun override(role: MotionRole, spec: MotionSpec?)
    fun observe(): LiveData<MotionState>
}
```

`MotionState` 同时暴露 `systemScale` 与 `reducedMotion`：

- `systemScale == 0` 或无障碍 reduced motion：所有 duration 归零；
- 插件注册的 profile 需用户启用，默认不自动应用；
- 宿主 Shell 的所有角色动画消费同一 token；
- 逐页覆盖可以后置，不在第一版同时做。

## 6. 反馈（音效/触感）

### 6.1 现状

API 9 有语义事件和触感；没有设置 UI、不能自定义声音资源；通知声音固定。

### 6.2 建议

```kotlin
sealed interface SoundSource {
    data class Host(val id: String) : SoundSource
    data class PluginAsset(val pluginId: String, val path: String) : SoundSource
}

data class FeedbackProfile(
    val id: String,
    val sounds: Map<FeedbackEvent, SoundSource>,
    val haptics: Map<HapticType, Int> = emptyMap(), // duration ms/amplitude
)

interface FeedbackService {
    fun registerProfile(profile: FeedbackProfile): Boolean
    fun applyProfile(id: String): Boolean
    fun override(event: FeedbackEvent, source: SoundSource?)
    fun settings(): FeedbackSettings
    fun updateSettings(settings: FeedbackSettings)
}
```

安全与平台边界：

- 插件音频只能从自身 APK assets 读取，路径规范化，禁止 `..`；
- 宿主统一音量/静音/耳机/勿扰；
- 通知声音不能通过 UI 音效服务修改；通知渠道声音只能引导用户到系统设置；
- 插件不能绕过系统静音；
- 自定义音效失败时回退宿主默认，不影响业务。

## 7. 插件依赖与整合包绑定

### 7.1 统一 descriptor

```kotlin
data class PluginRequirement(
    val id: String,
    val minVersion: String? = null,
    val maxVersion: String? = null,
    val optional: Boolean = false,
)

data class PluginDescriptor(
    val id: String,
    val requiresServices: Set<String> = emptySet(),
    val requiresPlugins: List<PluginRequirement> = emptyList(),
    val provides: Set<String> = emptySet(),
    val origin: PluginOrigin,
    val version: String? = null,
)
```

对 UI 插件 `plugin.json` 增加同名可选字段：

```json
{
  "requires": {
    "services": ["hita.eas"],
    "plugins": [
      {"id": "com.example.base", "minVersion": "0.2.0", "optional": false}
    ]
  },
  "uiContract": {
    "targetPreset": "hita-android-3.1.2",
    "supportedPresets": ["hita-android-3.1.2"],
    "layoutVersion": 1,
    "themeStyles": ["classic", "cyber"]
  }
}
```

### 7.2 解析语义

- 服务依赖：沿用现有 `requires`，缺失等待；
- 插件 id 依赖：按 DAG 波次激活，循环检测拒绝并进入 diagnostics；
- optional 缺失：插件仍激活，状态 `degraded`；
- 卸载 provider：不自动卸载 dependent，只标记 degraded；
- UI 插件依赖不满足：`RuntimeUiService` 不路由，回退内置页；
- preset 依赖：默认软约束，市场显示“未验证”，不自动卸载；
- `required` 仍是整合包组合级，不是插件依赖。

### 7.3 诊断

新增 `DependencyState`：

```text
satisfied / waiting / optional-missing / cycle / version-conflict / preset-unverified
```

写入 composition.json / diagnostics，不静默。

## 8. 通知与提醒

可做：

- 提醒提前分钟数改为设置项；
- 更多页文案读取实际值，不再固定“15 分钟”；
- 提供“打开系统通知渠道设置”的动作；
- 通知渠道创建时按用户选择初始化声音/振动。

不可安全做到：

- 已创建通知渠道的声音/重要性不能由应用事后修改；只能引导系统设置；
- 应用不能替代系统勿扰/静音/辅助功能。

## 9. 平台限制与非目标

不应伪装成可自定义：

- 启动图标/应用名：需要 Activity alias 或重打包，普通插件 API 不处理；
- 全局屏幕密度/系统字体：Android 系统级，应用不能覆盖；
- 任意字体文件：需宿主导入、校验、存储后再作为 token 提供；
- 插件替换宿主 Activity/Window/系统栏：违反当前 UI 插件容器边界；
- 插件绕过用户静音/无障碍：永不允许。

## 10. 分阶段落地

| 阶段 | 内容 | 验收 |
|---|---|---|
| A | HITA-Android 消费 `tabs`；统一 HITA-L/Android layout | 已完成：6 tab 顺序/隐藏/改名与编辑 UI 设备验证通过；HITA-L 不回归 |
| B | UI 插件 `requires`/`uiContract` + diagnostics | 已实现服务/插件依赖与 preset 软校验；依赖图 UI/自动重试待补 |
| C | Motion 全角色 + ThemeOverride + Feedback 设置 UI | 系统缩放 0 时动画归零；静音不可绕过 |
| D | 插件 chrome actions、通知设置入口 | 动作/标题/返回可验证；渠道设置跳转 |

## 11. 迁移与兼容

- API 版本：10（A 阶段后）；所有新字段可选，旧插件保持默认行为；
- 旧 `requires`（服务名）映射为 `requiresServices`；
- 未知 preset/tab 保留并标记，不静默删除；
- ABI 变更时插件索引必须更新 `apiAbi`；
- 用户覆盖数据独立于整合包，切换 preset 不丢；
- 所有自定义能力默认关闭或保持现有默认，避免升级惊扰。

## 12. 结论

优先级最高的是 **A：让整合包 `tabs` 真正生效**，因为它同时解决 HITA-Android 布局写死、
插件 tab 贡献和用户布局三个问题。其次是 **B：launcher 依赖契约**，否则插件无法安全声明
“基于哪个整合包界面”。主题/动效/反馈属于可逐步开放的视觉层，必须在用户/系统优先级和
Android 通知渠道限制下实现。
