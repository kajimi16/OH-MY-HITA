package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.Decision
import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hita.api.HookKey
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.NavRegistry
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.PluginHooks
import com.stupidtree.hita.api.PluginToolHandler
import com.stupidtree.hita.api.PluginToolRegistry
import com.stupidtree.hita.api.PluginToolSchema
import com.stupidtree.hita.api.PluginToolSpec
import com.stupidtree.hita.api.normalizeToolName
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.SearchTabRegistry
import com.stupidtree.hita.api.TabContribution
import com.stupidtree.hita.api.TabRegistry
import com.stupidtree.hita.api.TextNavEntry
import com.stupidtree.hita.api.ViewSearchTab
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
    private val textNavEntries = LinkedHashMap<String, TextNavEntry>()
    private val searchTabEntries = LinkedHashMap<String, SearchTab>()
    private val viewSearchTabEntries = LinkedHashMap<String, ViewSearchTab>()
    private val tabEntries = LinkedHashMap<String, TabContribution>()
    private val syncCodecs = LinkedHashMap<String, SyncCodec>()
    private val installedPlugins = LinkedHashMap<String, Plugin>()
    private val scopes = LinkedHashMap<String, PluginScope>()
    private val failures = LinkedHashMap<String, Throwable>()
    private val failurePhases = LinkedHashMap<String, PluginFailurePhase>()
    private val serviceProviders = LinkedHashMap<String, String>()
    private val descriptors = LinkedHashMap<String, PluginDescriptor>()
    private val pendingPlugins = LinkedHashMap<String, PendingInstall>()
    private val waitingPlugins = LinkedHashMap<String, MutableSet<String>>()
    private val conflicts = ArrayList<PluginConflict>()
    private val globalServices = LinkedHashSet<String>()
    private val availableServices = LinkedHashSet<String>()
    private data class HookRegistration(
        val pluginId: String,
        val listener: Decision<*>,
    )

    private val hookListeners = LinkedHashMap<String, MutableList<HookRegistration>>()
    private val hookFailures = ArrayList<PluginHookFailure>()
    private val toolConflicts = ArrayList<PluginToolConflict>()
    private val hookLock = Any()
    private val pluginToolBindings = LinkedHashMap<String, PluginToolBinding>()
    private val toolLock = Any()

    data class PluginToolBinding(
        val pluginId: String,
        val origin: PluginOrigin,
        val spec: PluginToolSpec,
        val handler: PluginToolHandler,
    )

    private data class PendingInstall(
        val plugin: Plugin,
        val descriptor: PluginDescriptor,
    )

    /** 宿主只读的导航视图。 */
    val nav: NavRegistry = object : NavRegistry {
        override fun register(entry: NavEntry) {
            navEntries[entry.id] = entry
        }

        override fun entries(): List<NavEntry> = navEntries.values.sortedBy { it.order }

        override fun find(id: String): NavEntry? = navEntries[id]

        override fun registerText(entry: TextNavEntry) {
            textNavEntries[entry.id] = entry
        }

        override fun textEntries(): List<TextNavEntry> =
            textNavEntries.values.sortedBy { it.order }
    }

    /** 宿主只读的搜索 tab 视图。 */
    val searchTabs: SearchTabRegistry = object : SearchTabRegistry {
        override fun register(tab: SearchTab) {
            searchTabEntries[tab.id] = tab
        }

        override fun entries(): List<SearchTab> = searchTabEntries.values.sortedBy { it.order }

        override fun registerView(tab: ViewSearchTab) {
            viewSearchTabEntries[tab.id] = tab
        }

        override fun viewEntries(): List<ViewSearchTab> =
            viewSearchTabEntries.values.sortedBy { it.order }
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

    fun installAllWithDescriptors(installations: List<PluginInstallation>) {
        installations.forEach { install(it.plugin, it.descriptor) }
    }

    /** 记录一次发生在 `install()` 之前的拒绝，供诊断与测试读取。 */
    fun reject(
        pluginId: String,
        reason: Throwable,
        phase: PluginFailurePhase = PluginFailurePhase.COMPATIBILITY,
    ) {
        failures[pluginId] = reason
        failurePhases[pluginId] = phase
        logger("plugin rejected: $pluginId", reason)
    }

    fun install(plugin: Plugin) {
        install(plugin, PluginDescriptor(plugin.id))
    }

    fun install(plugin: Plugin, descriptor: PluginDescriptor) {
        if (scopes.containsKey(plugin.id) || pendingPlugins.containsKey(plugin.id)) {
            logger("plugin already installed or waiting: ${plugin.id}", null)
            return
        }
        val missing = descriptor.requires.filterNot { it in availableServices }
        if (missing.isNotEmpty()) {
            pendingPlugins[plugin.id] = PendingInstall(plugin, descriptor)
            waitingPlugins[plugin.id] = missing.toMutableSet()
            failures.remove(plugin.id)
            failurePhases.remove(plugin.id)
            logger("plugin waiting for services: ${plugin.id} -> ${missing.joinToString()}", null)
            return
        }
        activate(plugin, descriptor)
        drainPending()
    }

    private fun activate(plugin: Plugin, descriptor: PluginDescriptor) {
        if (plugin.requiresApi > HITA_API_VERSION) {
            reject(
                plugin.id,
                IllegalStateException("requires api ${plugin.requiresApi}, kernel api $HITA_API_VERSION"),
                PluginFailurePhase.COMPATIBILITY,
            )
            return
        }
        val scope = PluginScope(plugin.id, descriptor.origin)
        scopes[plugin.id] = scope
        installedPlugins[plugin.id] = plugin
        try {
            plugin.install(scope)
            descriptors[plugin.id] = descriptor
            logDeclaredProvideMismatches(plugin.id, descriptor)
            // 同一 id 可能先前失败过（例如新版本装载失败后回滚到旧版本）。
            // 成功装载必须清掉旧记录，否则 PluginLoader 会把这次成功误判成失败。
            failures.remove(plugin.id)
            failurePhases.remove(plugin.id)
            logger("plugin installed: ${plugin.id}", null)
        } catch (t: Throwable) {
            scope.revokeAll()
            scopes.remove(plugin.id)
            installedPlugins.remove(plugin.id)
            descriptors.remove(plugin.id)
            failures[plugin.id] = t
            failurePhases[plugin.id] = PluginFailurePhase.INSTALL
            logger("plugin install failed: ${plugin.id}", t)
        }
    }

    /** 按“依赖已满足”的波次继续激活等待中的插件。 */
    private fun drainPending() {
        var progressed: Boolean
        do {
            progressed = false
            val iterator = pendingPlugins.entries.iterator()
            while (iterator.hasNext()) {
                val (id, pending) = iterator.next()
                val missing = pending.descriptor.requires.filterNot { it in availableServices }
                if (missing.isEmpty()) {
                    iterator.remove()
                    waitingPlugins.remove(id)
                    activate(pending.plugin, pending.descriptor)
                    progressed = true
                } else {
                    waitingPlugins[id] = missing.toMutableSet()
                }
            }
        } while (progressed)
    }

    fun plugin(pluginId: String): Plugin? = installedPlugins[pluginId]

    fun isInstalled(pluginId: String): Boolean = installedPlugins.containsKey(pluginId)

    /** 插件已提交但仍在等待 requires 中的服务。 */
    fun isWaiting(pluginId: String): Boolean = pendingPlugins.containsKey(pluginId)

    /** [purgeData] 为真时先让插件清理自己的持久化数据，再撤销注册。 */
    fun uninstall(pluginId: String, purgeData: Boolean = false) {
        if (pendingPlugins.remove(pluginId) != null) {
            waitingPlugins.remove(pluginId)
            logger("plugin cancelled while waiting: $pluginId", null)
            return
        }
        val plugin = installedPlugins[pluginId] ?: return
        val scope = scopes[pluginId] ?: return
        if (purgeData) {
            runCatching { plugin.purgeData() }
                .onFailure { logger("plugin purge failed: $pluginId", it) }
        }
        runCatching { plugin.uninstall(scope) }
            .onFailure { logger("plugin uninstall hook failed: $pluginId", it) }
        scope.revokeAll()
        scopes.remove(pluginId)
        installedPlugins.remove(pluginId)
        descriptors.remove(pluginId)
        conflicts.removeAll { it.existingProvider == pluginId || it.rejectedProvider == pluginId }
        synchronized(toolLock) {
            toolConflicts.removeAll { it.existingPluginId == pluginId || it.rejectedPluginId == pluginId }
        }
        synchronized(hookLock) {
            hookFailures.removeAll { it.pluginId == pluginId }
        }
        failures.remove(pluginId)
        failurePhases.remove(pluginId)
        rebuildAvailableServices()
        drainPending()
    }

    /** 服务可用集只由实际提供者重建，声明了 provides 但被冲突拒绝的插件不算可用。 */
    private fun rebuildAvailableServices() {
        availableServices.clear()
        availableServices.addAll(globalServices)
        serviceProviders.forEach { (service, provider) ->
            if (provider != HOST_PROVIDER && scopes.containsKey(provider)) {
                availableServices.add(service)
            }
        }
    }

    /**
     * 宿主自己提供的服务（不属于任何插件，卸载插件时不会被撤销）。
     * 插件通过 `ctx.service(key)` 读到同一个实例。
     */
    fun <T : Any> provideGlobal(key: ServiceKey<T>, impl: T) {
        services[key.name] = impl
        serviceProviders[key.name] = HOST_PROVIDER
        globalServices.add(key.name)
        availableServices.add(key.name)
        drainPending()
    }

    fun <T : Any> service(key: ServiceKey<T>): T? = services[key.name] as? T

    fun <T : Any> requireService(key: ServiceKey<T>): T =
        services[key.name] as? T
            ?: throw IllegalStateException("service not provided: ${key.name}")

    private fun registerPluginTool(
        pluginId: String,
        origin: PluginOrigin,
        spec: PluginToolSpec,
        handler: PluginToolHandler,
    ): AutoCloseable {
        PluginToolSchema.validationError(spec)?.let { error ->
            throw IllegalArgumentException("invalid plugin tool ${spec.name}: $error")
        }
        val key = normalizeToolName(spec.name)
        val binding = PluginToolBinding(pluginId, origin, spec, handler)
        synchronized(toolLock) {
            val existing = pluginToolBindings[key]
            if (existing != null && existing.pluginId != pluginId) {
                toolConflicts += PluginToolConflict(key, existing.pluginId, pluginId)
                logger("tool name conflict: $key kept by ${existing.pluginId}, rejected $pluginId", null)
                return AutoCloseable {}
            }
            pluginToolBindings[key] = binding
        }
        return AutoCloseable {
            synchronized(toolLock) {
                if (pluginToolBindings[key]?.handler === handler) {
                    pluginToolBindings.remove(key)
                }
            }
        }
    }

    /** 宿主读取当前已安装插件贡献的工具；按名称稳定排序。 */
    fun pluginTools(): List<PluginToolBinding> = synchronized(toolLock) {
        pluginToolBindings.values.toList()
    }.sortedBy { it.spec.name }

    private fun <T> registerHook(
        pluginId: String,
        key: HookKey<T>,
        listener: Decision<T>,
    ): AutoCloseable {
        val registration = HookRegistration(pluginId, listener)
        synchronized(hookLock) {
            val listeners = hookListeners.getOrPut(key.name) { mutableListOf() }
            listeners += registration
        }
        return AutoCloseable {
            synchronized(hookLock) {
                val listeners = hookListeners[key.name] ?: return@synchronized
                listeners.remove(registration)
                if (listeners.isEmpty()) hookListeners.remove(key.name)
            }
        }
    }

    fun <T> runHook(key: HookKey<T>, value: T): T {
        val listeners = synchronized(hookLock) {
            hookListeners[key.name]?.toList().orEmpty()
        }
        var failed = false
        val result = invokeHook(key.name, listeners, 0, value, value) { failed = true }
        return if (failed) value else result
    }

    private fun recordHookFailure(
        keyName: String,
        registration: HookRegistration,
        index: Int,
        t: Throwable,
    ) {
        synchronized(hookLock) {
            hookFailures += PluginHookFailure(
                hook = keyName,
                pluginId = registration.pluginId,
                listenerIndex = index,
                message = t.message ?: t::class.java.simpleName,
            )
            if (hookFailures.size > 100) hookFailures.removeAt(0)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> invokeHook(
        keyName: String,
        listeners: List<HookRegistration>,
        index: Int,
        value: T,
        original: T,
        onFailure: () -> Unit,
    ): T {
        if (index >= listeners.size) return value
        val registration = listeners[index]
        var delegated = false
        return try {
            val result = (registration.listener as Decision<T>).handle(value) { next ->
                if (delegated) throw IllegalStateException("hook next() called more than once")
                delegated = true
                invokeHook(keyName, listeners, index + 1, next, original, onFailure)
            }
            if (result == null) {
                recordHookFailure(
                    keyName,
                    registration,
                    index,
                    IllegalStateException("hook returned null"),
                )
                onFailure()
                original
            } else {
                result
            }
        } catch (t: Throwable) {
            logger("hook $keyName listener $index failed", t)
            recordHookFailure(keyName, registration, index, t)
            onFailure()
            original
        }
    }

    fun installedIds(): List<String> = installedPlugins.keys.toList()

    fun failures(): Map<String, Throwable> = failures.toMap()

    /** 当前内核状态的稳定快照；字段顺序不依赖 HashMap 遍历顺序。 */
    fun diagnostics(): PluginDiagnostics = PluginDiagnostics(
        installed = installedPlugins.entries
            .map { (id, plugin) ->
                val descriptor = descriptors[id]
                PluginSnapshot(
                    id = id,
                    requiresApi = plugin.requiresApi,
                    origin = descriptor?.origin ?: PluginOrigin.UNKNOWN,
                    version = descriptor?.version,
                    requires = descriptor?.requires.orEmpty().sorted(),
                    provides = descriptor?.provides.orEmpty().sorted(),
                )
            }
            .sortedBy { it.id },
        failed = failures.entries
            .map { (id, error) ->
                PluginFailure(
                    id = id,
                    phase = failurePhases[id] ?: PluginFailurePhase.INSTALL,
                    message = error.message ?: error::class.java.simpleName,
                )
            }
            .sortedBy { it.id },
        waiting = waitingPlugins.entries
            .map { (id, missing) -> PluginWaiting(id, missing.toList().sorted()) }
            .sortedBy { it.id },
        conflicts = conflicts.toList(),
        degraded = installedPlugins.keys
            .mapNotNull { id ->
                val missing = descriptors[id]?.requires
                    ?.filterNot { it in availableServices }
                    .orEmpty()
                if (missing.isEmpty()) null else PluginDegraded(id, missing.sorted())
            }
            .sortedBy { it.id },
        services = serviceProviders.entries
            .map { (service, provider) -> PluginServiceBinding(service, provider) }
            .sortedBy { it.service },
        hookFailures = synchronized(hookLock) { hookFailures.toList() },
        toolConflicts = synchronized(toolLock) { toolConflicts.toList() },
        declaredProvides = declaredProvideMismatches(),
    )

    /**
     * `provides` 只是清单提示：实际提供者以 `serviceProviders` 为准。
     * 这里只报告已安装插件的声明与实际不一致的条目，按插件 id、服务名稳定排序。
     */
    private fun declaredProvideMismatches(): List<PluginDeclaredProvides> =
        installedPlugins.keys
            .sorted()
            .flatMap { id ->
                descriptors[id]?.provides.orEmpty()
                    .sorted()
                    .mapNotNull { service ->
                        val actual = serviceProviders[service]
                        if (actual == id) null
                        else PluginDeclaredProvides(id, service, actual)
                    }
            }

    private fun logDeclaredProvideMismatches(pluginId: String, descriptor: PluginDescriptor) {
        descriptor.provides.sorted().forEach { service ->
            val actual = serviceProviders[service]
            if (actual != pluginId) {
                logger(
                    "plugin $pluginId declares '$service' but actual provider is ${actual ?: "<none>"}",
                    null,
                )
            }
        }
    }

    /**
     * 单个插件的注册作用域。所有 undo 都是幂等的移除/恢复动作，
     * 因此「安装到一半失败」也能安全回滚。
     */
    inner class PluginScope(
        private val pluginId: String,
        private val origin: PluginOrigin,
    ) : PluginContext {

        private val undoStack = ArrayList<() -> Unit>()
        private val undoLock = Any()
        @Volatile
        private var revoked = false

        private fun addUndo(undo: () -> Unit): Boolean = synchronized(undoLock) {
            if (revoked) false else {
                undoStack += undo
                true
            }
        }

        override fun <T : Any> service(key: ServiceKey<T>): T? = this@PluginHost.service(key)

        override fun <T : Any> provide(key: ServiceKey<T>, impl: T) {
            if (revoked) return
            val existingProvider = serviceProviders[key.name]
            if (existingProvider != null && existingProvider != pluginId) {
                conflicts += PluginConflict(key.name, existingProvider, pluginId)
                logger(
                    "service conflict: ${key.name} kept by $existingProvider, rejected $pluginId",
                    null,
                )
                return
            }
            val previous = services.put(key.name, impl)
            val previousProvider = serviceProviders.put(key.name, pluginId)
            availableServices.add(key.name)
            addUndo {
                // 宿主或其它插件后来接管该 key 时，卸载不能把它们的实现覆盖掉。
                if (serviceProviders[key.name] == pluginId) {
                    if (previous == null) {
                        services.remove(key.name)
                        serviceProviders.remove(key.name)
                        availableServices.remove(key.name)
                    } else {
                        services[key.name] = previous
                        if (previousProvider == null) serviceProviders.remove(key.name)
                        else serviceProviders[key.name] = previousProvider
                        availableServices.add(key.name)
                    }
                }
            }
        }

        override fun filesDir(): File =
            File(pluginsDir, pluginId).apply { mkdirs() }

        override val hooks: PluginHooks = object : PluginHooks {
            override fun <T> register(key: HookKey<T>, listener: Decision<T>): AutoCloseable {
                val handle = this@PluginHost.registerHook(pluginId, key, listener)
                if (!addUndo { handle.close() }) {
                    handle.close()
                    return AutoCloseable {}
                }
                return handle
            }

            override fun <T> run(key: HookKey<T>, value: T): T =
                if (revoked) value else this@PluginHost.runHook(key, value)
        }

        override val tools: PluginToolRegistry = object : PluginToolRegistry {
            override fun register(spec: PluginToolSpec, handler: PluginToolHandler): AutoCloseable {
                val handle = this@PluginHost.registerPluginTool(pluginId, origin, spec, handler)
                if (!addUndo { handle.close() }) {
                    handle.close()
                    return AutoCloseable {}
                }
                return handle
            }
        }

        override val nav: NavRegistry = object : NavRegistry {
            override fun register(entry: NavEntry) {
                if (revoked) return
                val previous = navEntries.put(entry.id, entry)
                addUndo {
                    if (previous == null) navEntries.remove(entry.id) else navEntries[entry.id] = previous
                }
            }

            override fun entries(): List<NavEntry> = this@PluginHost.nav.entries()

            override fun find(id: String): NavEntry? = this@PluginHost.nav.find(id)

            override fun registerText(entry: TextNavEntry) {
                if (revoked) return
                val previous = textNavEntries.put(entry.id, entry)
                addUndo {
                    if (previous == null) textNavEntries.remove(entry.id)
                    else textNavEntries[entry.id] = previous
                }
            }

            override fun textEntries(): List<TextNavEntry> =
                this@PluginHost.nav.textEntries()
        }

        override val searchTabs: SearchTabRegistry = object : SearchTabRegistry {
            override fun register(tab: SearchTab) {
                if (revoked) return
                val previous = searchTabEntries.put(tab.id, tab)
                addUndo {
                    if (previous == null) searchTabEntries.remove(tab.id)
                    else searchTabEntries[tab.id] = previous
                }
            }

            override fun entries(): List<SearchTab> = this@PluginHost.searchTabs.entries()

            override fun registerView(tab: ViewSearchTab) {
                if (revoked) return
                val previous = viewSearchTabEntries.put(tab.id, tab)
                addUndo {
                    if (previous == null) viewSearchTabEntries.remove(tab.id)
                    else viewSearchTabEntries[tab.id] = previous
                }
            }

            override fun viewEntries(): List<ViewSearchTab> =
                this@PluginHost.searchTabs.viewEntries()
        }

        override val tabs: TabRegistry = object : TabRegistry {
            override fun register(tab: TabContribution) {
                if (revoked) return
                val previous = tabEntries.put(tab.id, tab)
                addUndo {
                    if (previous == null) tabEntries.remove(tab.id) else tabEntries[tab.id] = previous
                }
            }

            override fun entries(): List<TabContribution> = this@PluginHost.tabs.entries()

            override fun find(id: String): TabContribution? = this@PluginHost.tabs.find(id)
        }

        override val sync: SyncRegistry = object : SyncRegistry {
            override fun register(codec: SyncCodec) {
                if (revoked) return
                val previous = syncCodecs.put(codec.key, codec)
                addUndo {
                    if (previous == null) syncCodecs.remove(codec.key)
                    else syncCodecs[codec.key] = previous
                }
            }

            override fun get(key: String): SyncCodec? = this@PluginHost.sync.get(key)

            override fun keys(): Set<String> = this@PluginHost.sync.keys()
        }

        fun revokeAll() {
            val actions = synchronized(undoLock) {
                revoked = true
                undoStack.toList().also { undoStack.clear() }
            }
            actions.asReversed().forEach { runCatching(it) }
        }
    }

    companion object {
        /** 诊断里表示服务由宿主 `provideGlobal` 提供。 */
        const val HOST_PROVIDER = "host"
    }
}
