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

```kotlin
ctx.nav.register(
    NavEntry(id = "theta.community", titleRes = R.string.xxx, iconRes = R.drawable.xxx, order = 30) { context ->
        Intent(context, ThetaActivity::class.java)   // 返回 null 表示本次不跳转
    }
)
```

### SearchTab（搜索页 tab）

```kotlin
ctx.searchTabs.register(SearchTab(SearchTabIds.USER, R.string.tab_user, order = 20) { UserListFragment() })
```

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

## 版本策略

- `HITA_API_VERSION` 是契约的二进制版本；任何破坏性改动必须递增。
- 内核装载时检查 `requiresApi <= HITA_API_VERSION`，不满足直接拒绝并记录原因。
- 当前版本 **2**（2：移除 `PluginContext.appContext`，新增 `sync`）。

变更记录：

| 版本 | 变更 |
|---|---|
| 1 | 首个版本 |
| 2 | 移除 `appContext`；新增 `SyncRegistry` |

## 运行时插件（T1）

除构建期插件外，还支持从 dex 动态装载**逻辑插件**：

- 只能贡献服务 / 同步表 / 主题等纯逻辑；不能声明四大组件、不能携带资源
- 安装流程：索引 → 下载 dex → 校验 sha256 → `DexClassLoader` → `install(ctx)`
- 索引与发布流程见 [`market/README.md`](../../market/README.md)
- 与宿主同进程，拥有宿主全部权限，只应上架可信代码

## 最小示例

```kotlin
class HelloPlugin : Plugin {
    override val id = "com.example.hello"
    override val requiresApi = 2

    override fun install(ctx: PluginContext) {
        ctx.service(ThemeController.KEY)?.setNightMode(true)
    }

    override fun uninstall(ctx: PluginContext) {
        ctx.service(ThemeController.KEY)?.setNightMode(false)
    }
}
```
