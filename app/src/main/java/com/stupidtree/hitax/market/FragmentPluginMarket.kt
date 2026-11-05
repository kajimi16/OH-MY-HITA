package com.stupidtree.hitax.market

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.FragmentPluginMarketBinding
import com.stupidtree.hitax.databinding.ItemMarketPluginCardBinding
import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.plugin.PluginPrefs
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import com.stupidtree.style.base.BaseFragment

/**
 * 插件市场：分为「查询并下载」与「我的插件」两栏。
 *
 * - 查询并下载：按当前插件源列出索引里的插件，可搜索、安装/更新。
 * - 我的插件：内置插件 + 已安装的界面/逻辑/APK 插件，可启用、停用、卸载、更新。
 */
class FragmentPluginMarket : BaseFragment<PluginMarketViewModel, FragmentPluginMarketBinding>() {

    override fun getViewModelClass(): Class<PluginMarketViewModel> = PluginMarketViewModel::class.java

    override fun initViewBinding(): FragmentPluginMarketBinding =
        FragmentPluginMarketBinding.inflate(layoutInflater)

    private var latestBuiltin: List<BuiltinPluginManager.Row> = emptyList()
    private var latestDex: List<PluginMarketViewModel.Item> = emptyList()
    private var latestUiInstalled: List<InstalledUiPlugin> = emptyList()
    private var latestUiAvailable: List<UiMarketPlugin> = emptyList()
    private var latestApk: List<ApkPlugin> = emptyList()
    private var latestPendingUpdates = 0
    private var query: String = ""
    private var selectedTab = 0
    private fun uiPrefs() =
        requireContext().getSharedPreferences("market_ui", android.content.Context.MODE_PRIVATE)

    override fun initViews(view: View) {
        selectedTab = uiPrefs().getInt(
            KEY_TAB,
            if (uiPrefs().getBoolean(KEY_MINE, false)) TAB_MINE else TAB_DISCOVER,
        )
        query = uiPrefs().getString(KEY_QUERY, "").orEmpty()
        viewModel.builtinPlugins.observe(this) { latestBuiltin = it; renderAll() }
        viewModel.items.observe(this) { latestDex = it; renderAll() }
        viewModel.uiPlugins.observe(this) { latestUiInstalled = it; renderAll() }
        viewModel.availableUiPlugins.observe(this) { latestUiAvailable = it; renderAll() }
        viewModel.apkPlugins.observe(this) { latestApk = it; renderAll() }
        viewModel.sources.observe(this) { renderAll() }
        viewModel.pendingUpdates.observe(this) { latestPendingUpdates = it; renderAll() }

        binding?.tabDiscover?.setOnClickListener { showTab(TAB_DISCOVER) }
        binding?.tabMine?.setOnClickListener { showTab(TAB_MINE) }
        binding?.tabGuide?.setOnClickListener { showTab(TAB_GUIDE) }
        binding?.guidePublish?.setOnClickListener {
            openUrl("https://github.com/kajimi16/hita-community-plugins/issues/new?template=publish-request.yml")
        }
        binding?.guideApi?.setOnClickListener {
            openUrl("https://github.com/kajimi16/OH-MY-HITA/blob/master/docs/api/plugin-api.md")
        }
        binding?.guideTemplate?.setOnClickListener {
            openUrl("https://github.com/kajimi16/hita-plugin-template")
        }
        binding?.guideCommunity?.setOnClickListener {
            openUrl("https://github.com/kajimi16/hita-community-plugins")
        }
        binding?.marketSource?.setOnClickListener { showSourceDialog() }
        binding?.updateAll?.setOnClickListener { viewModel.updateAll() }
        binding?.discoverSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty()
                uiPrefs().edit().putString(KEY_QUERY, query).apply()
                renderDiscover()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.reloadUi.observe(this) { reload ->
            if (reload) {
                viewModel.consumeReloadUi()
                requireActivity().recreate()
            }
        }
        viewModel.status.observe(this) { text ->
            binding?.status?.text = text
            binding?.status?.visibility = if (text == null) View.GONE else View.VISIBLE
        }
        viewModel.message.observe(this) { text ->
            text ?: return@observe
            Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
        binding?.discoverSearch?.setText(query)
        showTab(selectedTab)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun showTab(tab: Int) {
        selectedTab = tab
        uiPrefs().edit().putInt(KEY_TAB, tab).apply()
        val tabs = listOf(binding?.tabDiscover, binding?.tabMine, binding?.tabGuide)
        val scrolls = listOf(binding?.discoverScroll, binding?.mineScroll, binding?.guideScroll)
        tabs.forEachIndexed { index, view ->
            view?.alpha = if (index == tab) 1f else 0.5f
            view?.setTypeface(
                null,
                if (index == tab) android.graphics.Typeface.BOLD
                else android.graphics.Typeface.NORMAL,
            )
        }
        scrolls.forEachIndexed { index, view ->
            view?.visibility = if (index == tab) View.VISIBLE else View.GONE
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            )
        }
    }

    // ---- 渲染 ----

    private fun renderAll() {
        renderBuiltin()
        renderDiscover()
        renderMine()
    }

    private fun renderBuiltin() {
        renderGrid(binding?.builtinContainer, latestBuiltin) { card, row ->
            card.name.text = row.name
            card.meta.text = if (row.installed) "内置插件 · 已启用" else "内置插件 · 已停用"
            card.toggle.visibility = View.GONE
            card.action.visibility = View.VISIBLE
            card.action.text = if (row.installed) "停用" else "启用"
            card.action.setOnClickListener {
                viewModel.setBuiltinEnabled(row, !row.installed)
            }
        }
    }

    private sealed class DiscoverEntry {
        data class Ui(val plugin: UiMarketPlugin, val installedVersion: String?) : DiscoverEntry()
        data class Dex(val item: PluginMarketViewModel.Item) : DiscoverEntry()
    }

    private fun renderDiscover() {
        val installedUi = latestUiInstalled.associateBy { it.id }
        val entries = mutableListOf<DiscoverEntry>()
        latestUiAvailable
            .filter { matches(query, it.name, it.description, it.tags?.joinToString(" ")) }
            .forEach { entries += DiscoverEntry.Ui(it, installedUi[it.id]?.version) }
        latestDex
            .filter { matches(query, it.entry.name, it.entry.description, it.entry.tags?.joinToString(" ")) }
            .forEach { entries += DiscoverEntry.Dex(it) }

        binding?.discoverEmpty?.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding?.marketSource?.text = viewModel.activeSource().name
        renderGrid(binding?.discoverContainer, entries) { card, entry ->
            when (entry) {
                is DiscoverEntry.Ui -> bindDiscoverUi(card, entry.plugin, entry.installedVersion)
                is DiscoverEntry.Dex -> bindDiscoverDex(card, entry.item)
            }
        }
    }

    private fun bindDiscoverUi(
        card: ItemMarketPluginCardBinding,
        plugin: UiMarketPlugin,
        installedVersion: String?,
    ) {
        val update = installedVersion != null && PluginVersion.isNewer(plugin.version, installedVersion)
        card.name.text = plugin.name
        card.meta.text = "界面插件 · v${plugin.version}" + if (update) " · 可更新" else ""
        card.toggle.visibility = View.GONE
        card.action.visibility = View.VISIBLE
        card.action.text = when {
            installedVersion == null -> getString(R.string.plugin_ui_install_action)
            update -> getString(R.string.plugin_ui_update)
            else -> "已安装"
        }
        card.action.isEnabled = installedVersion == null || update
        card.action.setOnClickListener { installUi(plugin) }
        card.root.setOnClickListener {
            showDetails(
                name = plugin.name,
                version = plugin.version,
                description = plugin.description,
                changelog = plugin.changelog,
                size = plugin.size,
                minHostApi = plugin.minHostApi ?: 1,
                author = plugin.author,
                tags = plugin.tags,
                repo = plugin.repo,
                updatedAt = plugin.updatedAt,
                actionLabel = card.action.text?.toString(),
                action = if (card.action.isEnabled) ({ installUi(plugin) }) else null,
            )
        }
    }

    private fun bindDiscoverDex(card: ItemMarketPluginCardBinding, item: PluginMarketViewModel.Item) {
        card.name.text = item.entry.name
        card.meta.text = "在线插件 · v${item.entry.version} · api ${item.entry.apiVersion}" +
            if (item.update) " · 可更新" else ""
        card.toggle.visibility = View.GONE
        card.action.visibility = View.VISIBLE
        card.action.text = when {
            !item.installed -> getString(R.string.plugin_ui_install_action)
            item.update -> getString(R.string.plugin_ui_update)
            else -> "已安装"
        }
        card.action.isEnabled = !item.installed || item.update
        card.action.setOnClickListener { installDex(item.entry) }
        card.root.setOnClickListener {
            showDetails(
                name = item.entry.name,
                version = item.entry.version,
                description = item.entry.description,
                changelog = item.entry.changelog,
                size = item.entry.size,
                minHostApi = item.entry.minHostApi ?: 1,
                author = item.entry.author,
                tags = item.entry.tags,
                repo = item.entry.repo,
                updatedAt = item.entry.updatedAt,
                actionLabel = card.action.text?.toString(),
                action = if (card.action.isEnabled) ({ installDex(item.entry) }) else null,
            )
        }
    }

    private sealed class MineEntry {
        data class Ui(val plugin: InstalledUiPlugin, val update: Boolean) : MineEntry()
        data class Dex(val item: PluginMarketViewModel.Item) : MineEntry()
        data class Apk(val plugin: ApkPlugin) : MineEntry()
    }

    private fun renderMine() {
        val availableUi = latestUiAvailable.associateBy { it.id }
        val installedDex = latestDex.filter { it.installed }
        val entries = mutableListOf<MineEntry>()
        latestUiInstalled.forEach { plugin ->
            val available = availableUi[plugin.id]
            entries += MineEntry.Ui(
                plugin,
                available != null && PluginVersion.isNewer(available.version, plugin.version),
            )
        }
        installedDex.forEach { entries += MineEntry.Dex(it) }
        latestApk.forEach { entries += MineEntry.Apk(it) }

        binding?.marketEmpty?.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        val hasUpdates = entries.any {
            (it is MineEntry.Ui && it.update) || (it is MineEntry.Dex && it.item.update)
        } || latestPendingUpdates > 0
        binding?.updateAll?.visibility = if (hasUpdates) View.VISIBLE else View.GONE

        renderGrid(binding?.marketContainer, entries) { card, entry ->
            when (entry) {
                is MineEntry.Ui -> bindUiInstalled(card, entry.plugin, entry.update)
                is MineEntry.Dex -> bindDexInstalled(card, entry.item)
                is MineEntry.Apk -> bindApk(card, entry.plugin)
            }
        }
    }

    private fun bindUiInstalled(
        card: ItemMarketPluginCardBinding,
        plugin: InstalledUiPlugin,
        update: Boolean,
    ) {
        card.name.text = plugin.name
        card.meta.text = "界面插件 · v${plugin.version}" + if (update) " · 可更新" else ""
        card.action.visibility = View.VISIBLE
        card.action.text = getString(R.string.plugin_builtin_uninstall)
        card.action.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.plugin_builtin_uninstall)
                .setMessage(plugin.name)
                .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.uninstallUiPlugin(plugin) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        card.toggle.visibility = View.VISIBLE
        bindToggle(card, plugin.enabled) { enabled ->
            viewModel.setUiPluginEnabled(plugin, enabled)
        }
        card.root.setOnClickListener {
            if (!plugin.enabled) {
                Toast.makeText(requireContext(), "插件已停用", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(
                android.content.Intent(
                    requireContext(),
                    com.stupidtree.hitax.uiplugin.PluginUiActivity::class.java,
                ).putExtra(
                    com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_PLUGIN_ID,
                    plugin.id,
                )
            )
        }
    }

    private fun bindDexInstalled(card: ItemMarketPluginCardBinding, item: PluginMarketViewModel.Item) {
        card.name.text = item.entry.name
        card.meta.text = "在线插件 · v${item.entry.version} · api ${item.entry.apiVersion}" +
            if (item.update) " · 可更新" else ""
        card.action.visibility = View.VISIBLE
        card.action.text = "卸载"
        card.action.setOnClickListener { viewModel.uninstall(item.entry) }
        card.toggle.visibility = View.VISIBLE
        bindToggle(card, item.enabled) { enabled ->
            viewModel.setMarketEnabled(item.entry, enabled)
        }
    }

    private fun bindApk(card: ItemMarketPluginCardBinding, plugin: ApkPlugin) {
        val disabledKey = "apk:${plugin.packageName}"
        card.name.text = plugin.name
        card.meta.text = "独立 APK · v${plugin.versionName}"
        card.action.visibility = View.VISIBLE
        card.action.text = getString(R.string.plugin_builtin_uninstall)
        card.action.setOnClickListener {
            runCatching {
                startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_DELETE,
                        android.net.Uri.parse("package:${plugin.packageName}"),
                    )
                )
            }
        }
        card.toggle.visibility = View.VISIBLE
        bindToggle(card, !PluginPrefs.isDisabled(requireContext(), disabledKey)) { enabled ->
            viewModel.setApkEnabled(plugin.packageName, enabled)
        }
        card.root.setOnClickListener {
            if (PluginPrefs.isDisabled(requireContext(), disabledKey)) {
                Toast.makeText(requireContext(), "插件已停用", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val launch: () -> Unit = {
                val intent = android.content.Intent()
                    .setComponent(plugin.entry)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { requireContext().startActivity(intent) }
                Unit
            }
            if (viewModel.isVerified(plugin.id)) launch() else confirmUntrusted(plugin.name, launch)
        }
    }

    // ---- 通用 ----

    private fun matches(query: String, vararg fields: String?): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return fields.any { it?.contains(q, ignoreCase = true) == true }
    }

    private fun installUi(plugin: UiMarketPlugin) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.plugin_ui_warning)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.installUiPluginFromUrl(plugin, viewModel.indexBase())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun installDex(entry: MarketPlugin) {
        if (viewModel.isVerified(entry.id)) {
            viewModel.install(entry)
        } else {
            confirmUntrusted(entry.name) { viewModel.install(entry) }
        }
    }

    private fun confirmUntrusted(name: String, action: () -> Unit) {
        com.stupidtree.style.widgets.PopUpText()
            .setTitle(R.string.plugin_untrusted_title)
            .setText(getString(R.string.plugin_untrusted_message, name))
            .setOnConfirmListener(object : com.stupidtree.style.widgets.PopUpText.OnConfirmListener {
                override fun OnConfirm() = action()
            })
            .show(parentFragmentManager, "plugin_untrusted")
    }

    private fun showDetails(
        name: String,
        version: String,
        description: String?,
        changelog: String?,
        size: Long,
        minHostApi: Int,
        author: String? = null,
        tags: List<String>? = null,
        repo: String? = null,
        updatedAt: String? = null,
        actionLabel: String? = null,
        action: (() -> Unit)? = null,
    ) {
        val parts = mutableListOf<String>()
        parts += description?.takeIf { it.isNotBlank() }
            ?: getString(R.string.plugin_details_no_description)
        if (!changelog.isNullOrBlank()) {
            parts += getString(R.string.plugin_details_changelog, changelog)
        }
        if (size > 0) {
            parts += getString(R.string.plugin_details_size, (size / 1024).toInt())
        }
        parts += getString(R.string.plugin_details_min_api, minHostApi)
        if (!author.isNullOrBlank()) parts += getString(R.string.plugin_details_author, author)
        if (!tags.isNullOrEmpty()) {
            parts += getString(R.string.plugin_details_tags, tags.joinToString(" · "))
        }
        if (!updatedAt.isNullOrBlank()) {
            parts += getString(R.string.plugin_details_updated, updatedAt)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.plugin_details_title, name, version))
            .setMessage(parts.joinToString("\n\n"))
        if (action != null) {
            if (actionLabel != null) {
                dialog.setPositiveButton(actionLabel) { _, _ -> action() }
            } else {
                dialog.setPositiveButton(android.R.string.ok) { _, _ -> action() }
            }
            dialog.setNegativeButton(android.R.string.cancel, null)
        } else {
            dialog.setPositiveButton(android.R.string.ok) { _, _ -> }
        }
        if (!repo.isNullOrBlank()) {
            dialog.setNeutralButton(R.string.plugin_details_repo) { _, _ ->
                runCatching {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(repo)))
                }
            }
        }
        dialog.show()
    }

    private fun showSourceDialog() {
        val sources = viewModel.sources.value.orEmpty()
        val active = viewModel.activeSource()
        val labels = sources.map {
            it.name + if (it.url == active.url && it.name == active.name) "（当前）" else ""
        }.toMutableList()
        labels += getString(R.string.plugin_market_source_add)
        val canRemove = sources.indexOfFirst { it.url == active.url && it.name == active.name } > 0
        if (canRemove) labels += getString(R.string.plugin_market_source_remove)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.plugin_market_source)
            .setItems(labels.toTypedArray()) { _, which ->
                when {
                    which < sources.size -> viewModel.selectSource(sources[which])
                    which == sources.size -> showAddSourceDialog()
                    else -> viewModel.removeSource(active)
                }
            }
            .show()
    }

    private fun showAddSourceDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.plugin_market_source_hint)
            setSingleLine(true)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.plugin_market_source_add)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val error = viewModel.addSource("", input.text?.toString().orEmpty())
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun <T> renderGrid(
        container: LinearLayout?,
        items: List<T>,
        binder: (ItemMarketPluginCardBinding, T) -> Unit,
    ) {
        container ?: return
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        items.chunked(2).forEach { pair ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            pair.forEach { item ->
                val card = ItemMarketPluginCardBinding.inflate(inflater, row, false)
                binder(card, item)
                row.addView(
                    card.root,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
            }
            if (pair.size == 1) {
                row.addView(View(requireContext()), LinearLayout.LayoutParams(0, 1, 1f))
            }
            container.addView(row)
        }
    }

    companion object {
        private const val KEY_MINE = "mine"
        private const val KEY_TAB = "tab"
        private const val KEY_QUERY = "query"

        private const val TAB_DISCOVER = 0
        private const val TAB_MINE = 1
        private const val TAB_GUIDE = 2
    }

    private fun bindToggle(
        card: ItemMarketPluginCardBinding,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ) {
        card.toggle.setOnCheckedChangeListener(null)
        card.toggle.isChecked = checked
        card.toggle.setOnCheckedChangeListener { _, value -> onChange(value) }
    }
}
