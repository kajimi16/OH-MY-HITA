# 插件 API 参考

插件只依赖 `:api`（以及它导出的 `:model`），不依赖宿主。

```
:api     契约：Plugin / PluginContext / ServiceKey / NavEntry / SearchTab / SyncCodec / ThemeController
:model   共享数据模型：Timetable / EventItem / TermSubject …
:kernel  内核：装载、卸载、注册表回滚（不依赖 Android，可单测）
:app     宿主：UI、Worker、Widget
```

## Plugin

```kotlin
interface Plugin {
    val id: String                       // 全局唯一，建议反向域名
    val requiresApi: Int get() = 1       // 依赖的契约版本
    val displayName: String get() = id
    fun install(ctx: PluginContext)
    fun uninstall(ctx: PluginContext) {} // 可选；此时注册尚未回滚，仍能读服务
}
```

- `install` 抛异常 → 本次已产生的全部注册按相反顺序撤销，插件标记为失败，其它插件不受影响。
- `uninstall` 先执行钩子，再统一回滚注册；钩子抛异常不会中断回滚。

## PluginContext

| 成员 | 说明 |
|---|---|
| `service(key)` | 取服务；未注册返回 `null` |
| `provide(key, impl)` | 注册服务；同名覆盖，卸载时恢复上一个实现 |
| `filesDir()` | 该插件私有目录 |
| `nav` | 导航贡献点（`NavEntry`） |
| `searchTabs` | 搜索结果页 tab 贡献点（`SearchTab`） |
| `sync` | 云同步数据表贡献点（`SyncCodec`） |

宿主还可以通过 `PluginHost.provideGlobal(key, impl)` 提供内置能力，插件用 `ctx.service(key)` 读取，
插件卸载不会移除这类服务（例如 `ThemeController`）。

## 贡献点

### NavEntry（导航）

构建期插件可用资源 id：

```kotlin
ctx.nav.register(
    NavEntry(id = "theta.community", titleRes = R.string.xxx, iconRes = R.drawable.xxx, order = 30) { context ->
        Intent(context, ThetaActivity::class.java)   // 返回 null 表示本次不跳转
    }
)
```

运行时 dex 插件没有资源表，用 `textTitle` 与可选 `icon`（Drawable）：

```kotlin
ctx.nav.register(
    NavEntry(
        id = "demo.nav",
        titleRes = 0,
        iconRes = 0,
        order = 200,
        createIntent = { context -> Intent(context, SomeActivity::class.java) },
        textTitle = "示例入口",
    )
)
```

### SearchTab（搜索页 tab）

```kotlin
ctx.searchTabs.register(SearchTab(SearchTabIds.USER, R.string.tab_user, order = 20) { UserListFragment() })
```

运行时 dex 插件不能安全地返回 Fragment（恢复时类不在 classpath），改用 `ViewSearchTab`：

```kotlin
ctx.searchTabs.registerView(
    ViewSearchTab(
        id = "demo.search",
        title = "示例搜索",
        order = 200,
        createView = { context -> TextView(context).apply { text = "插件搜索页" } },
        onQuery = { text -> /* 用户输入变化 */ },
    )
)
```

宿主用固定 Fragment 承载这个 View，并对 `onQuery` 转发搜索词。

`SearchTabIds` 是宿主与插件共用的语义 id，宿主用它把 `SearchType` 映射到具体 tab。

### SyncCodec（云同步表）

```kotlin
interface SyncCodec {
    val key: String                                  // "timetable" / "subject" / "event"
    fun export(ids: List<String>): List<JsonObject>
    fun save(ids: List<String>, data: List<String>)
    fun delete(ids: List<String>)
    fun clear()
}
```

新增一种可同步数据 = 注册一个 codec，宿主一行不用改。

## 宿主能力

### ThemeController

```kotlin
interface ThemeController { fun setNightMode(night: Boolean) }
// 用法：ctx.service(ThemeController.KEY)?.setNightMode(true)
```

宿主侧已经把线程编排放进实现里（post 到主线程），插件无需关心 Android 主线程约束。

## 插件模板

最小可用的逻辑插件模板（含 Gradle 配置、d8 打包、市场索引生成）：

<https://github.com/kajimi16/hita-plugin-template>

## 独立 APK 插件（T2）

需要界面或独立进程边界的插件，做成单独安装的 APK，而不是 dex：

```xml
<application>
    <meta-data android:name="hita.plugin.id" android:value="com.example.sample" />
    <meta-data android:name="hita.plugin.apiVersion" android:value="2" />

    <activity android:name=".SamplePluginActivity" android:exported="true">
        <intent-filter>
            <action android:name="com.stupidtree.hita.action.PLUGIN_APK" />
            <category android:name="android.intent.category.DEFAULT" />
        </intent-filter>
    </activity>
</application>
```

宿主侧：

- 通过 `queryIntentActivities` 发现插件，读取 `<meta-data>` 得到 id 与契约版本
- 版本高于 `HITA_API_VERSION` 的插件显示为「版本不兼容」，按钮禁用
- 「打开」= 直接启动插件自己的 Activity（插件拥有完整界面与资源）
- 声明 `<queries>` 以适配 Android 11+ 的包可见性

### 信任模型

- 所有插件（T1 dex 与 T2 APK）默认标记为「未验证」
- 未验证插件在安装 / 打开前会弹窗提示：运行后拥有应用完整权限
- 与 OH-MY-HITA 一起核验过的半官方插件，由维护者加入
  `app/src/main/assets/verified_plugins.json` 白名单，界面上显示「已验证」
- 不做签名校验：白名单即信任边界，随应用版本发布

示例模块：`:plugin-apk-sample`（`./gradlew :plugin-apk-sample:assembleDebug`）。

## 版本策略

- `HITA_API_VERSION` 是契约的二进制版本；任何破坏性改动必须递增。
- 内核装载时检查 `requiresApi <= HITA_API_VERSION`，不满足直接拒绝并记录原因。
- 当前版本 **5**（5：运行时多主 tab、文本导航项、View 搜索 tab）。

变更记录：

| 版本 | 变更 |
|---|---|
| 1 | 首个版本 |
| 2 | 移除 `appContext`；新增 `SyncRegistry` |
| 3 | 新增 `ComposeUiPlugin`、`PluginUiHost`、`RuntimeUiService`，支持运行时界面插件 |
| 4 | 新增 `CAP_MAIN_TAB`，运行时界面插件可占用主界面 tab |
| 5 | 支持多个运行时主 tab；`NavEntry` 支持字符串标题；新增 `ViewSearchTab` |

## 运行时逻辑插件（T1）

除构建期插件外，还支持从 dex 动态装载**逻辑插件**：

- 只能贡献服务 / 同步表 / 主题等纯逻辑；不能声明四大组件、不能携带资源
- 安装流程：索引 → 下载 dex → 校验 sha256 → `DexClassLoader` → `install(ctx)`
- 索引与发布流程见 [`market/README.md`](../../market/README.md)
- 与宿主同进程，拥有宿主全部权限，只应上架可信代码

## 运行时界面插件（T1 UI）

界面插件是**独立 APK**（`assets/plugin.json` + dex），由宿主的 Compose 容器承载：

```kotlin
interface ComposeUiPlugin {
    val id: String
    val displayName: String
    val requiresApi: Int get() = 1
    fun createView(context: Context, host: PluginUiHost): View
    fun onDestroy() {}
}
```

`assets/plugin.json` 示例：

```json
{
  "id": "com.example.ui",
  "name": "示例界面",
  "version": "0.1.0",
  "entryClass": "com.example.ui.MyUiPlugin",
  "apiVersion": 3,
  "composeVersion": "1.6.8",
  "capabilities": ["hita.ui.resourceSearch"]
}
```

- 宿主按 `capabilities` 路由：声明了对应能力的插件启用时优先接管，返回 `false` 或插件损坏时回退内置页面。
- `mainTabs` 可选，支持多个主界面 tab：
  ```json
  "mainTabs": [
    {"id": "anime", "title": "番剧", "order": 100, "capability": "hita.ui.mainTab"},
    {"id": "anime2", "title": "追番", "order": 110, "capability": "hita.ui.mainTab"}
  ]
  ```
  每个 tab 打开时宿主会把 `tabId` / `capability` / `tabTitle` 放进 `PluginUiHost.arguments`。
  未声明 `mainTabs` 但带 `hita.ui.mainTab` 能力的旧插件，宿主按插件名自动生成一个 tab。
- 界面插件不注册 Activity，也不能携带资源表；所有 UI 用 Compose 代码构建。
- 安装前校验 `sha256`，并要求 `apiVersion`、`composeVersion` 与宿主一致。
- 信任模型与逻辑插件相同：索引是信任源，`verified_plugins.json` 目前为空。

## 最小示例

```kotlin
interface GreetingService {
    fun greet(): String
}

class HelloPlugin : Plugin {
    override val id = "com.example.hello"
    override val requiresApi = 2

    override fun install(ctx: PluginContext) {
        // 无副作用：只注册一个服务，演示契约与回滚。
        ctx.provide(
            ServiceKey<GreetingService>("demo.greeting"),
            object : GreetingService {
                override fun greet() = "hello from plugin"
            },
        )
    }
}
```
