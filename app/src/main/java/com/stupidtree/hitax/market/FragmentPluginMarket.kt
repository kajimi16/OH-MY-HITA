package com.stupidtree.hitax.market

import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.FragmentPluginMarketBinding
import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.plugin.PluginPrefs
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import com.stupidtree.hitax.uiplugin.PluginUiActivity
import com.stupidtree.style.base.BaseFragment

/**
 * 插件市场主页面。
 *
 * 视图只消费 [PluginMarketViewModel] 的 LiveData：
 * tab/筛选/搜索 -> [MarketListBuilder] -> [MarketAdapter]。
 * 安装、启停、卸载仍由 ViewModel 执行；详情和筛选面板不持有业务状态。
 */
class FragmentPluginMarket :
    BaseFragment<PluginMarketViewModel, FragmentPluginMarketBinding>(),
    MarketAdapter.Listener {

    private lateinit var adapter: MarketAdapter
    private lateinit var bindingRef: FragmentPluginMarketBinding
    private var selectedTab = MarketTab.EXPLORE
    private var filters = MarketFilters()
    private var query = ""
    private var loading = true
    private var loadError: String? = null
    private var status: String? = null
    private var latestBuiltin: List<BuiltinPluginManager.Row> = emptyList()
    private var latestDex: List<PluginMarketViewModel.Item> = emptyList()
    private var latestUiInstalled: List<InstalledUiPlugin> = emptyList()
    private var latestUiAvailable: List<UiMarketPlugin> = emptyList()
    private var latestPresets: List<MarketPreset> = emptyList()
    private var latestApk: List<ApkPlugin> = emptyList()
    private var latestStats: Map<String, MarketStat> = emptyMap()
    private var latestPendingUpdates = 0
    private var latestActivePresetId: String? = null
    private var pendingPluginId: String? = null
    private var pendingAutoInstall = false

    override fun getViewModelClass(): Class<PluginMarketViewModel> = PluginMarketViewModel::class.java

    override fun initViewBinding(): FragmentPluginMarketBinding =
        FragmentPluginMarketBinding.inflate(layoutInflater)

    override fun initViews(view: View) {
        bindingRef = binding ?: return
        pendingPluginId = arguments?.getString(PluginMarketActivity.ARG_PLUGIN_ID)
        pendingAutoInstall = arguments?.getBoolean(PluginMarketActivity.ARG_AUTO_INSTALL, false) == true
        val prefs = requireContext().getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE)
        selectedTab = when (prefs.getInt(KEY_TAB_V2, 0)) {
            1 -> MarketTab.INSTALLED
            2 -> MarketTab.UPDATES
            else -> MarketTab.EXPLORE
        }
        query = prefs.getString(KEY_QUERY, "").orEmpty()

        adapter = MarketAdapter(this)
        bindingRef.list.layoutManager = LinearLayoutManager(requireContext())
        bindingRef.list.adapter = adapter

        bindingRef.toolbar.setNavigationIcon(R.drawable.ic_market_back_24)
        bindingRef.toolbar.navigationIcon?.setTint(textColor(R.attr.textColorPrimary))
        bindingRef.toolbar.setNavigationOnClickListener { requireActivity().finish() }
        bindingRef.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> {
                    viewModel.refresh()
                    true
                }
                R.id.action_sources -> {
                    showSources()
                    true
                }
                R.id.action_guide -> {
                    MarketGuideSheet(requireContext()).show()
                    true
                }
                else -> false
            }
        }

        setupTabs()
        bindingRef.searchEdit.setText(query)
        bindingRef.searchEdit.setSelection(bindingRef.searchEdit.text?.length ?: 0)
        bindingRef.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                prefs.edit().putString(KEY_QUERY, query).apply()
                render()
            }
        })
        bindingRef.filterButton.setOnClickListener {
            clearSearchFocus()
            showFilters()
        }
        bindingRef.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        bindingRef.swipeRefresh.setColorSchemeColors(textColor(R.attr.colorPrimary))
        bindingRef.stateAction.setOnClickListener { }
        bindingRef.list.setHasFixedSize(false)

        observe()
        render()
    }

    private fun setupTabs() {
        val b = bindingRef
        listOf(
            R.string.market_tab_explore,
            R.string.market_tab_installed,
            R.string.market_tab_updates,
        ).forEach { b.tabs.addTab(b.tabs.newTab().setText(it)) }
        val position = when (selectedTab) {
            MarketTab.EXPLORE -> 0
            MarketTab.INSTALLED -> 1
            MarketTab.UPDATES -> 2
        }
        b.tabs.getTabAt(position)?.select()
        b.tabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                selectedTab = when (tab.position) {
                    1 -> MarketTab.INSTALLED
                    2 -> MarketTab.UPDATES
                    else -> MarketTab.EXPLORE
                }
                filters = filters.normalized(selectedTab)
                clearSearchFocus()
                requireContext().getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE)
                    .edit().putInt(KEY_TAB_V2, tab.position).apply()
                render()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun observe() {
        viewModel.builtinPlugins.observe(this) { latestBuiltin = it; render() }
        viewModel.items.observe(this) { latestDex = it; render() }
        viewModel.uiPlugins.observe(this) { latestUiInstalled = it; render() }
        viewModel.availableUiPlugins.observe(this) { plugins ->
            latestUiAvailable = plugins
            render()
            viewModel.refreshPluginStats(plugins.map { it.id })
        }
        viewModel.pluginStats.observe(this) { latestStats = it; render() }
        viewModel.presets.observe(this) { latestPresets = it; render() }
        viewModel.activePresetId.observe(this) { latestActivePresetId = it; render() }
        viewModel.apkPlugins.observe(this) { latestApk = it; render() }
        viewModel.pendingUpdates.observe(this) { latestPendingUpdates = it; render() }
        viewModel.loading.observe(this) { loading = it; render() }
        viewModel.loadError.observe(this) { loadError = it; render() }
        viewModel.status.observe(this) { status = it }
        viewModel.applyProgress.observe(this) { renderProgress(it) }
        viewModel.reloadUi.observe(this) { reload ->
            if (reload) {
                viewModel.consumeReloadUi()
                requireActivity().recreate()
            }
        }
        viewModel.presetSwitch.observe(this) { target ->
            target ?: return@observe
            viewModel.consumePresetSwitch()
            val context = requireContext()
            val intent = if (!target.shell.isNullOrBlank()) {
                PresetShellRouter.intent(context, target.shell)
            } else {
                Intent(context, com.stupidtree.hitax.ui.main.MainActivity::class.java)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            requireActivity().finish()
        }
        viewModel.message.observe(this) { text ->
            text ?: return@observe
            Snackbar.make(bindingRef.root, text, Snackbar.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    private fun render() {
        val b = binding ?: return
        if (!::adapter.isInitialized) return
        filters = filters.normalized(selectedTab)
        val apkDisabled = latestApk.mapNotNull { plugin ->
            if (PluginPrefs.isDisabled(requireContext(), "apk:${plugin.packageName}")) {
                plugin.packageName
            } else {
                null
            }
        }.toSet()
        val state = MarketState(
            presets = latestPresets,
            activePresetId = latestActivePresetId,
            availableUi = latestUiAvailable,
            installedUi = latestUiInstalled,
            dex = latestDex,
            builtins = latestBuiltin,
            apk = latestApk,
            apkDisabled = apkDisabled,
            pendingUpdates = latestPendingUpdates,
        )
        val items = MarketListBuilder.build(selectedTab, filters, query, state)
        adapter.indexBase = viewModel.indexBase()
        adapter.submitList(items)
        val pendingId = pendingPluginId
        if (pendingId != null) {
            bindingRef.list.post { openPendingPlugin(items, pendingId) }
        }

        val hasContent = items.any { it !is MarketItem.Section }
        b.filterButton.isVisible = selectedTab != MarketTab.UPDATES
        val filterActive = filters.activeCount() > 0 || query.isNotBlank()
        val contentCount = items.count { it !is MarketItem.Section }
        b.resultCount.isVisible = filterActive && !loading
        if (b.resultCount.isVisible) {
            b.resultCount.text = getString(R.string.market_result_count, contentCount)
        }
        b.skeleton.isVisible = loading && !hasContent
        b.swipeRefresh.isRefreshing = loading && hasContent
        b.stateView.isVisible = !loading && !hasContent
        if (b.stateView.isVisible) {
            val error = loadError
            if (error != null) {
                b.stateTitle.setText(R.string.market_error_title)
                b.stateMessage.text = getString(R.string.market_error_message, error)
                b.stateAction.isVisible = true
                b.stateAction.setText(R.string.market_retry)
                b.stateAction.setOnClickListener { viewModel.refresh() }
            } else {
                when {
                    filterActive -> {
                        b.stateTitle.setText(R.string.market_empty_title)
                        b.stateMessage.setText(R.string.market_empty_message)
                    }
                    selectedTab == MarketTab.UPDATES -> {
                        b.stateTitle.setText(R.string.market_updates_empty_title)
                        b.stateMessage.setText(R.string.market_updates_empty_message)
                    }
                    selectedTab == MarketTab.INSTALLED -> {
                        b.stateTitle.setText(R.string.market_installed_empty_title)
                        b.stateMessage.setText(R.string.market_installed_empty_message)
                    }
                    else -> {
                        b.stateTitle.setText(R.string.market_explore_empty_title)
                        b.stateMessage.setText(R.string.market_explore_empty_message)
                    }
                }
                b.stateAction.isVisible = filterActive
                if (filterActive) {
                    b.stateAction.setText(R.string.market_clear_filters)
                    b.stateAction.setOnClickListener { clearFilters() }
                }
            }
        }
        b.filterButton.text = if (filters.activeCount() > 0) {
            getString(R.string.market_filter_active, filters.activeCount())
        } else {
            getString(R.string.market_filter)
        }
        updateBadge()
    }

    private fun renderProgress(progress: PluginMarketViewModel.SwitchProgress?) {
        val b = binding ?: return
        b.progressCard.isVisible = progress != null
        if (progress != null) {
            b.progressMessage.text = progress.message
            b.progressPercent.text = "${progress.percent}%"
            b.progressBar.progress = progress.percent
        }
    }

    private fun updateBadge() {
        val b = binding ?: return
        val installedUi = latestUiInstalled.associateBy { it.id }
        val uiUpdates = latestUiAvailable.count { plugin ->
            val installed = installedUi[plugin.id]
            installed != null && PluginVersion.isNewer(plugin.version, installed.version)
        }
        val count = uiUpdates + latestDex.count { it.update } + latestPendingUpdates
        val tab = b.tabs.getTabAt(2) ?: return
        if (count <= 0) {
            tab.removeBadge()
            return
        }
        val badge = tab.orCreateBadge
        badge.text = count.toString()
        badge.isVisible = true
    }

    private fun clearSearchFocus() {
        val b = binding ?: return
        val imm = requireContext().getSystemService(android.view.inputmethod.InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(b.searchEdit.windowToken, 0)
        b.searchEdit.clearFocus()
    }

    private fun clearFilters() {
        filters = MarketFilters()
        query = ""
        binding?.searchEdit?.setText("")
        render()
    }

    private fun showFilters() {
        MarketFilterSheet(requireContext(), selectedTab, filters) {
            filters = it
            render()
        }.show()
    }

    // ---- Adapter callbacks ----

    override fun onOpen(item: MarketItem) {
        when (item) {
            is MarketItem.Preset -> showPresetDetail(item)
            is MarketItem.Ui -> showUiDetail(item)
            is MarketItem.Dex -> showDexDetail(item)
            is MarketItem.Builtin -> showBuiltinDetail(item)
            is MarketItem.Apk -> showApkDetail(item)
            is MarketItem.Section -> Unit
        }
    }

    override fun onAction(item: MarketItem) {
        when (item) {
            is MarketItem.Preset -> Unit
            is MarketItem.Ui -> if (item.installed == null || item.update) installUi(item.plugin)
            is MarketItem.Dex -> if (!item.item.installed || item.item.update) installDex(item.item.entry)
            else -> Unit
        }
    }

    override fun onToggle(item: MarketItem, checked: Boolean) {
        when (item) {
            is MarketItem.Ui -> item.installed?.let { viewModel.setUiPluginEnabled(it, checked) }
            is MarketItem.Dex -> viewModel.setMarketEnabled(item.item.entry, checked)
            is MarketItem.Builtin -> viewModel.setBuiltinEnabled(item.row, checked)
            is MarketItem.Apk -> viewModel.setApkEnabled(item.plugin.packageName, checked)
            else -> Unit
        }
    }

    override fun onSectionAction(action: SectionAction) {
        when (action) {
            SectionAction.UPDATE_ALL -> viewModel.updateAll()
            SectionAction.CLEAR_FILTERS -> clearFilters()
        }
    }

    // ---- 详情 ----

    private fun openPendingPlugin(items: List<MarketItem>, pluginId: String) {
        if (pendingPluginId != pluginId) return
        val autoInstall = pendingAutoInstall
        val ui = items.filterIsInstance<MarketItem.Ui>()
            .firstOrNull { it.plugin.id == pluginId }
        if (ui != null) {
            pendingPluginId = null
            pendingAutoInstall = false
            if (autoInstall && ui.installed == null) installUi(ui.plugin) else showUiDetail(ui)
            return
        }
        val dex = items.filterIsInstance<MarketItem.Dex>()
            .firstOrNull { it.item.entry.id == pluginId }
        if (dex != null) {
            pendingPluginId = null
            pendingAutoInstall = false
            if (autoInstall && !dex.item.installed) installDex(dex.item.entry) else showDexDetail(dex)
        }
    }

    private fun showUiDetail(item: MarketItem.Ui) {
        val plugin = item.plugin
        val installed = item.installed
        val sameVersion = installed?.version == plugin.version
        val update = installed != null && !sameVersion && PluginVersion.isNewer(plugin.version, installed.version)
        val verified = viewModel.isVerified(plugin.id)
        val family = plugin.family ?: plugin.id
        val alternatives = if (plugin.versionOptions().size > 1) {
            plugin.versionOptions().map { plugin.forVersion(it) }
        } else {
            latestUiAvailable
                .filter { (it.family ?: it.id) == family }
                .ifEmpty { listOf(plugin) }
        }
        val secondary = buildList {
            if (installed != null) {
                add(MarketDetailAction(getString(R.string.market_action_uninstall)) { confirmUninstallUi(installed) })
                if (installed.enabled && update) {
                    add(MarketDetailAction(getString(R.string.market_action_open)) { openUiPlugin(installed) })
                } else if (!installed.enabled) {
                    add(MarketDetailAction(getString(R.string.market_status_enabled)) {
                        viewModel.setUiPluginEnabled(installed, true)
                    })
                }
            }
        }
        val primary = when {
            installed == null -> MarketDetailAction(
                getString(R.string.market_action_install_version, plugin.version),
            ) { installUi(plugin) }
            !sameVersion -> MarketDetailAction(
                getString(R.string.market_action_install_version, plugin.version),
            ) { installUi(plugin) }
            installed.enabled -> MarketDetailAction(getString(R.string.market_action_open)) { openUiPlugin(installed) }
            else -> null
        }
        val meta = buildList {
            plugin.size.takeIf { it > 0 }?.let { add(getString(R.string.market_detail_size, formatSize(it))) }
            plugin.minHostApi?.let { add(getString(R.string.market_detail_api, it)) }
            plugin.author?.takeIf { it.isNotBlank() }?.let { add(getString(R.string.market_detail_author, it)) }
            plugin.updatedAt?.takeIf { it.isNotBlank() }?.let { add(getString(R.string.market_detail_updated, it)) }
            if (installed != null) {
                add("已安装 v${installed.version}")
            }
        }
        lateinit var sheet: MarketDetailSheet
        sheet = MarketDetailSheet(
            requireContext(),
            MarketDetailModel(
                title = plugin.name,
                version = plugin.version,
                kind = getString(R.string.market_kind_ui) + if (update) " · 可更新" else "",
                description = plugin.description,
                changelog = plugin.changelog,
                meta = meta,
                tags = plugin.tags.orEmpty(),
                trustLabel = if (verified) {
                    getString(R.string.market_detail_trust_verified)
                } else {
                    getString(R.string.market_detail_trust_unverified)
                },
                trustVerified = verified,
                repo = plugin.repo,
                logo = plugin.logo,
                indexBase = viewModel.indexBase(),
                primaryLabel = primary?.label,
                primary = primary?.onClick,
                secondary = secondary,
                versionOptions = alternatives.map { it.version },
                selectedVersion = plugin.version,
                onVersionSelected = { version ->
                    alternatives.firstOrNull { it.version == version }?.let {
                        sheet.dismiss()
                        showUiDetail(item.copy(plugin = it))
                    }
                },
            ),
        )
        sheet.show()
    }

    private fun showDexDetail(item: MarketItem.Dex) {
        val entry = item.item.entry
        val family = entry.family ?: entry.id
        val alternatives = if (entry.versionOptions().size > 1) {
            entry.versionOptions().map { entry.forVersion(it) }
        } else {
            latestDex
                .map { it.entry }
                .filter { (it.family ?: it.id) == family }
                .ifEmpty { listOf(entry) }
        }
        val latestVersion = alternatives.maxWithOrNull { a, b ->
            PluginVersion.compare(a.version, b.version)
        }?.version
        val verified = viewModel.isVerified(entry.id)
        val secondary = buildList {
            if (item.item.installed) {
                add(MarketDetailAction(getString(R.string.market_action_uninstall)) { confirmUninstallDex(entry) })
            }
        }
        val primary = when (
            MarketVersionAction.resolve(
                installed = item.item.installed,
                selectedVersion = entry.version,
                latestVersion = latestVersion,
                updateAvailable = item.item.update,
            )
        ) {
            MarketVersionAction.Kind.INSTALL -> MarketDetailAction(
                getString(R.string.market_action_install_version, entry.version),
            ) {
                installDex(entry)
            }
            MarketVersionAction.Kind.UPDATE -> MarketDetailAction(
                getString(R.string.market_action_update),
            ) {
                installDex(entry)
            }
            MarketVersionAction.Kind.NONE -> null
        }
        val meta = buildList {
            add("API ${entry.apiVersion}")
            entry.size.takeIf { it > 0 }?.let { add(getString(R.string.market_detail_size, formatSize(it))) }
            entry.minHostApi?.let { add(getString(R.string.market_detail_api, it)) }
            entry.author?.takeIf { it.isNotBlank() }?.let { add(getString(R.string.market_detail_author, it)) }
            entry.updatedAt?.takeIf { it.isNotBlank() }?.let { add(getString(R.string.market_detail_updated, it)) }
        }
        lateinit var sheet: MarketDetailSheet
        sheet = MarketDetailSheet(
            requireContext(),
            MarketDetailModel(
                title = entry.name,
                version = entry.version,
                kind = getString(R.string.market_kind_dex) + if (item.item.update) " · 可更新" else "",
                description = entry.description,
                changelog = entry.changelog,
                meta = meta,
                tags = entry.tags.orEmpty(),
                trustLabel = if (verified) {
                    getString(R.string.market_detail_trust_verified)
                } else {
                    getString(R.string.market_detail_trust_unverified)
                },
                trustVerified = verified,
                repo = entry.repo,
                logo = entry.logo,
                indexBase = viewModel.indexBase(),
                primaryLabel = primary?.label,
                primary = primary?.onClick,
                secondary = secondary,
                versionOptions = alternatives.map { it.version },
                selectedVersion = entry.version,
                onVersionSelected = { version ->
                    alternatives.firstOrNull { it.version == version }?.let {
                        sheet.dismiss()
                        showDexDetail(item.copy(item = item.item.copy(entry = it)))
                    }
                },
            ),
        )
        sheet.show()
    }

    private fun showPresetDetail(item: MarketItem.Preset) {
        val preset = item.preset
        val family = preset.familyId()
        val alternatives = latestPresets
            .filter { it.familyId() == family }
            .ifEmpty { listOf(preset) }
        val builtin = latestBuiltin.associateBy { it.id }
        val uiInstalled = latestUiInstalled.associateBy { it.id }
        val uiAvailable = latestUiAvailable.associateBy { it.id }
        val dexInstalled = latestDex.associateBy { it.entry.id }
        val dexAvailable = latestDex.associateBy { it.entry.id }
        val states = preset.refs().map { ref ->
            val (label, state) = when (ref.normalizedKind()) {
                "builtin" -> {
                    val row = builtin[ref.id]
                    (row?.name ?: ref.id) to when {
                        row == null -> "内置缺失"
                        row.installed -> "已启用"
                        else -> "未启用"
                    }
                }
                "ui" -> {
                    val installed = uiInstalled[ref.id]
                    val available = uiAvailable[ref.id]
                    (available?.name ?: installed?.name ?: ref.id) to when {
                        installed != null -> "已安装 v${installed.version}"
                        available != null -> "未安装"
                        else -> "索引缺失"
                    }
                }
                "dex" -> {
                    val installed = dexInstalled[ref.id]
                    val available = dexAvailable[ref.id]
                    (available?.entry?.name ?: installed?.entry?.name ?: ref.id) to when {
                        installed?.installed == true -> "已安装 v${installed.entry.version}"
                        available != null -> "未安装"
                        else -> "索引缺失"
                    }
                }
                else -> ref.id to "未知类型"
            }
            "· $label — $state"
        }
        val meta = buildList {
            preset.source?.takeIf { it.isNotBlank() }?.let { add("来源 $it") }
            add("包含 ${preset.refs().size} 个插件")
            preset.minHostApi?.let { add(getString(R.string.market_detail_api, it)) }
            if (item.active) add(getString(R.string.market_status_active))
        }
        val secondary = emptyList<MarketDetailAction>()
        val primary = if (item.active) {
            null
        } else {
            MarketDetailAction(
                getString(
                    if (latestActivePresetId == null) R.string.market_action_apply
                    else R.string.market_action_switch,
                ),
            ) { viewModel.applyPreset(preset) }
        }
        lateinit var sheet: MarketDetailSheet
        sheet = MarketDetailSheet(
            requireContext(),
            MarketDetailModel(
                title = preset.name,
                version = preset.version,
                kind = getString(R.string.market_kind_preset) + if (item.active) " · 使用中" else "",
                description = preset.description,
                changelog = null,
                meta = meta,
                tags = preset.tags.orEmpty(),
                presetPlugins = states,
                repo = preset.repo,
                indexBase = viewModel.indexBase(),
                primaryLabel = primary?.label,
                primary = primary?.onClick,
                secondary = secondary,
                versionOptions = alternatives.map { it.version },
                selectedVersion = preset.version,
                onVersionSelected = { version ->
                    alternatives.firstOrNull { it.version == version }?.let {
                        sheet.dismiss()
                        showPresetDetail(
                            item.copy(
                                preset = it,
                                active = it.id == latestActivePresetId,
                            ),
                        )
                    }
                },
            ),
        )
        sheet.show()
    }

    private fun showBuiltinDetail(item: MarketItem.Builtin) {
        val row = item.row
        val primary = MarketDetailAction(
            getString(if (row.installed) R.string.market_action_disable else R.string.market_action_enable),
        ) { viewModel.setBuiltinEnabled(row, !row.installed) }
        MarketDetailSheet(
            requireContext(),
            MarketDetailModel(
                title = row.name,
                version = null,
                kind = getString(R.string.market_kind_builtin),
                description = null,
                changelog = null,
                meta = listOf(
                    if (row.installed) getString(R.string.market_section_enabled)
                    else getString(R.string.market_status_disabled),
                ),
                primaryLabel = primary.label,
                primary = primary.onClick,
            ),
        ).show()
    }

    private fun showApkDetail(item: MarketItem.Apk) {
        val plugin = item.plugin
        val verified = viewModel.isVerified(plugin.id)
        val primary = MarketDetailAction(
            getString(
                if (item.enabled) R.string.market_action_open else R.string.market_status_enabled,
            ),
        ) {
            if (item.enabled) openApk(plugin) else viewModel.setApkEnabled(plugin.packageName, true)
        }
        val secondary = buildList {
            add(MarketDetailAction(getString(R.string.market_action_uninstall)) { uninstallApk(plugin) })
        }
        MarketDetailSheet(
            requireContext(),
            MarketDetailModel(
                title = plugin.name,
                version = plugin.versionName,
                kind = getString(R.string.market_kind_apk),
                description = null,
                changelog = null,
                meta = listOf("API ${plugin.apiVersion}", plugin.packageName),
                trustLabel = if (verified) {
                    getString(R.string.market_detail_trust_verified)
                } else {
                    getString(R.string.market_detail_trust_unverified)
                },
                trustVerified = verified,
                primaryLabel = primary.label,
                primary = primary.onClick,
                secondary = secondary,
            ),
        ).show()
    }

    // ---- 动作 ----

    private fun installUi(plugin: UiMarketPlugin) {
        MaterialAlertDialogBuilder(requireContext())
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

    private fun openUiPlugin(plugin: InstalledUiPlugin) {
        if (!plugin.enabled) {
            Snackbar.make(bindingRef.root, "插件已停用", Snackbar.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(requireContext(), PluginUiActivity::class.java)
                .putExtra(PluginUiActivity.EXTRA_PLUGIN_ID, plugin.id),
        )
    }

    private fun openApk(plugin: ApkPlugin) {
        val launch: () -> Unit = {
            val intent = Intent()
                .setComponent(plugin.entry)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { requireContext().startActivity(intent) }
            Unit
        }
        if (viewModel.isVerified(plugin.id)) launch() else confirmUntrusted(plugin.name, launch)
    }

    private fun confirmUninstallUi(plugin: InstalledUiPlugin) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.plugin_builtin_uninstall)
            .setMessage(plugin.name)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.uninstallUiPlugin(plugin) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmUninstallDex(entry: MarketPlugin) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.market_action_uninstall)
            .setMessage(entry.name)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.uninstall(entry) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun uninstallApk(plugin: ApkPlugin) {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${plugin.packageName}")),
            )
        }
    }

    private fun showSources() {
        val sources = viewModel.sources.value.orEmpty()
        if (sources.isEmpty()) return
        MarketSourcesSheet(
            requireContext(),
            sources,
            viewModel.activeSource(),
            status,
            object : MarketSourcesSheet.Listener {
                override fun onSelect(source: MarketSource) {
                    viewModel.selectSource(source)
                }

                override fun onRemove(source: MarketSource) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.market_source_remove_title)
                        .setMessage(getString(R.string.market_source_remove_message, source.name))
                        .setPositiveButton(R.string.market_source_remove) { _, _ ->
                            viewModel.removeSource(source)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }

                override fun onAdd() = showAddSource()
            },
        ).show()
    }

    private fun showAddSource() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.market_source_hint)
            setSingleLine(true)
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        val container = FrameLayout(requireContext()).apply {
            setPadding(dp(8), dp(4), dp(8), 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.market_source_add)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val error = viewModel.addSource("", input.text?.toString().orEmpty())
                if (error != null) {
                    Snackbar.make(bindingRef.root, error, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun formatSize(bytes: Long): String =
        if (bytes >= 1024 * 1024) {
            String.format(java.util.Locale.CHINA, "%.1f MB", bytes / 1024f / 1024f)
        } else {
            "${(bytes / 1024).coerceAtLeast(1)} KB"
        }

    private fun textColor(attr: Int): Int =
        com.google.android.material.color.MaterialColors.getColor(bindingRef.root, attr)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        viewModel.syncActivePreset()
        viewModel.refresh()
    }

    companion object {
        private const val UI_PREFS = "market_ui"
        private const val KEY_TAB_V2 = "tab_v2"
        private const val KEY_QUERY = "query"
    }
}
