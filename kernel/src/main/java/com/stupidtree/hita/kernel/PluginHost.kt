package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.NavRegistry
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.SearchTabRegistry
import com.stupidtree.hita.api.TabContribution
import com.stupidtree.hita.api.TabRegistry
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hita.api.SyncCodec
import com.stupidtree.hita.api.SyncRegistry
import java.io.File

/**
 * 微内核：只负责「装/卸插件」和「持有注册表」。
 *
 * 设计约束：
 * - 不出现任何业务概念（没有课程、没有学期、没有用户）；也不依赖 Android，
 *   因此可以在 JVM 上直接跑单元测试。
 * - 插件的每一次注册都是一条可撤销记录（effect），装载失败或卸载时按相反顺序回滚。
 * - 一个插件装载失败不影响其它插件，失败原因保留在 [failures] 里。
 */
class PluginHost(
    private val pluginsDir: File,
    private val logger: (message: String, error: Throwable?) -> Unit = { _, _ -> },
) {

    private val services = LinkedHashMap<String, Any>()
    private val navEntries = LinkedHashMap<String, NavEntry>()
    private val searchTabEntries = LinkedHashMap<String, SearchTab>()
    private val tabEntries = LinkedHashMap<String, TabContribution>()
    private val syncCodecs = LinkedHashMap<String, SyncCodec>()
    private val installedPlugins = LinkedHashMap<String, Plugin>()
    private val scopes = LinkedHashMap<String, PluginScope>()
    private val failures = LinkedHashMap<String, Throwable>()

    /** 宿主只读的导航视图。 */
    val nav: NavRegistry = object : NavRegistry {
        override fun register(entry: NavEntry) {
            navEntries[entry.id] = entry
        }

        override fun entries(): List<NavEntry> = navEntries.values.sortedBy { it.order }

        override fun find(id: String): NavEntry? = navEntries[id]
    }

    /** 宿主只读的搜索 tab 视图。 */
    val searchTabs: SearchTabRegistry = object : SearchTabRegistry {
        override fun register(tab: SearchTab) {
            searchTabEntries[tab.id] = tab
        }

        override fun entries(): List<SearchTab> = searchTabEntries.values.sortedBy { it.order }
    }

    /** 宿主只读的主界面 tab 视图。 */
    val tabs: TabRegistry = object : TabRegistry {
        override fun register(tab: TabContribution) {
            tabEntries[tab.id] = tab
        }

        override fun entries(): List<TabContribution> = tabEntries.values.sortedBy { it.order }

        override fun find(id: String): TabContribution? = tabEntries[id]
    }

    /** 宿主只读的同步表视图。 */
    val sync: SyncRegistry = object : SyncRegistry {
        override fun register(codec: SyncCodec) {
            syncCodecs[codec.key] = codec
        }

        override fun get(key: String): SyncCodec? = syncCodecs[key]

        override fun keys(): Set<String> = syncCodecs.keys.toSet()
    }

    fun installAll(plugins: List<Plugin>) {
        plugins.forEach { install(it) }
    }

    fun install(plugin: Plugin) {
        if (scopes.containsKey(plugin.id)) {
            logger("plugin already installed: ${plugin.id}", null)
            return
        }
        if (plugin.requiresApi > HITA_API_VERSION) {
            failures[plugin.id] =
                IllegalStateException("requires api ${plugin.requiresApi}, kernel api $HITA_API_VERSION")
            logger("plugin rejected: ${plugin.id}", null)
            return
        }
        val scope = PluginScope(plugin.id)
        scopes[plugin.id] = scope
        installedPlugins[plugin.id] = plugin
        try {
            plugin.install(scope)
            logger("plugin installed: ${plugin.id}", null)
        } catch (t: Throwable) {
            scope.revokeAll()
            scopes.remove(plugin.id)
            installedPlugins.remove(plugin.id)
            failures[plugin.id] = t
            logger("plugin install failed: ${plugin.id}", t)
        }
    }

    fun uninstall(pluginId: String) {
        val plugin = installedPlugins[pluginId] ?: return
        val scope = scopes[pluginId] ?: return
        runCatching { plugin.uninstall(scope) }
            .onFailure { logger("plugin uninstall hook failed: $pluginId", it) }
        scope.revokeAll()
        scopes.remove(pluginId)
        installedPlugins.remove(pluginId)
    }

    /**
     * 宿主自己提供的服务（不属于任何插件，卸载插件时不会被撤销）。
     * 插件通过 `ctx.service(key)` 读到同一个实例。
     */
    fun <T : Any> provideGlobal(key: ServiceKey<T>, impl: T) {
        services[key.name] = impl
    }

    fun <T : Any> service(key: ServiceKey<T>): T? = services[key.name] as? T

    fun <T : Any> requireService(key: ServiceKey<T>): T =
        services[key.name] as? T
            ?: throw IllegalStateException("service not provided: ${key.name}")

    fun installedIds(): List<String> = installedPlugins.keys.toList()

    fun failures(): Map<String, Throwable> = failures.toMap()

    /**
     * 单个插件的注册作用域。所有 undo 都是幂等的移除/恢复动作，
     * 因此「安装到一半失败」也能安全回滚。
     */
    inner class PluginScope(private val pluginId: String) : PluginContext {

        private val undoStack = ArrayList<() -> Unit>()

        override fun <T : Any> service(key: ServiceKey<T>): T? = this@PluginHost.service(key)

        override fun <T : Any> provide(key: ServiceKey<T>, impl: T) {
            val previous = services.put(key.name, impl)
            undoStack += {
                if (previous == null) services.remove(key.name) else services[key.name] = previous
            }
        }

        override fun filesDir(): File =
            File(pluginsDir, pluginId).apply { mkdirs() }

        override val nav: NavRegistry = object : NavRegistry {
            override fun register(entry: NavEntry) {
                val previous = navEntries.put(entry.id, entry)
                undoStack += {
                    if (previous == null) navEntries.remove(entry.id) else navEntries[entry.id] = previous
                }
            }

            override fun entries(): List<NavEntry> = this@PluginHost.nav.entries()

            override fun find(id: String): NavEntry? = this@PluginHost.nav.find(id)
        }

        override val searchTabs: SearchTabRegistry = object : SearchTabRegistry {
            override fun register(tab: SearchTab) {
                val previous = searchTabEntries.put(tab.id, tab)
                undoStack += {
                    if (previous == null) searchTabEntries.remove(tab.id)
                    else searchTabEntries[tab.id] = previous
                }
            }

            override fun entries(): List<SearchTab> = this@PluginHost.searchTabs.entries()
        }

        override val tabs: TabRegistry = object : TabRegistry {
            override fun register(tab: TabContribution) {
                val previous = tabEntries.put(tab.id, tab)
                undoStack += {
                    if (previous == null) tabEntries.remove(tab.id) else tabEntries[tab.id] = previous
                }
            }

            override fun entries(): List<TabContribution> = this@PluginHost.tabs.entries()

            override fun find(id: String): TabContribution? = this@PluginHost.tabs.find(id)
        }

        override val sync: SyncRegistry = object : SyncRegistry {
            override fun register(codec: SyncCodec) {
                val previous = syncCodecs.put(codec.key, codec)
                undoStack += {
                    if (previous == null) syncCodecs.remove(codec.key)
                    else syncCodecs[codec.key] = previous
                }
            }

            override fun get(key: String): SyncCodec? = this@PluginHost.sync.get(key)

            override fun keys(): Set<String> = this@PluginHost.sync.keys()
        }

        fun revokeAll() {
            undoStack.asReversed().forEach { runCatching(it) }
            undoStack.clear()
        }
    }
}
