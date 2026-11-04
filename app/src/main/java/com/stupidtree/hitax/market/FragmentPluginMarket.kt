package com.stupidtree.hitax.market

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stupidtree.hitax.databinding.FragmentPluginMarketBinding
import com.stupidtree.hitax.databinding.ItemMarketPluginCardBinding
import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import com.stupidtree.style.base.BaseFragment

/**
 * 「插件」主界面：与「今日」「时间表」「功能中心」并列。
 *
 * 每个条目半屏宽（两列网格），右侧竖排「卸载」按钮与启用开关；
 * 包名等开发者信息不再展示。
 */
class FragmentPluginMarket : BaseFragment<PluginMarketViewModel, FragmentPluginMarketBinding>() {

    override fun getViewModelClass(): Class<PluginMarketViewModel> = PluginMarketViewModel::class.java

    override fun initViewBinding(): FragmentPluginMarketBinding =
        FragmentPluginMarketBinding.inflate(layoutInflater)

    private val pickUiPluginApk =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            AlertDialog.Builder(requireContext())
                .setTitle(com.stupidtree.hitax.R.string.plugin_ui_section)
                .setMessage(com.stupidtree.hitax.R.string.plugin_ui_warning)
                .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.installUiPlugin(uri) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

    override fun initViews(view: View) {
        viewModel.items.observe(this) { renderMarket(it) }
        viewModel.apkPlugins.observe(this) { renderApkPlugins(it) }
        viewModel.builtinPlugins.observe(this) { renderBuiltin(it) }
        viewModel.uiPlugins.observe(this) { renderUiRows(it, latestUiAvailable) }
        viewModel.availableUiPlugins.observe(this) { renderUiRows(latestUiInstalled, it) }
        binding?.installUiPlugin?.setOnClickListener {
            pickUiPluginApk.launch(
                arrayOf(
                    "application/vnd.android.package-archive",
                    "application/octet-stream",
                )
            )
        }
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    // ---- 两列网格 ----

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
                row.addView(
                    View(requireContext()),
                    LinearLayout.LayoutParams(0, 1, 1f),
                )
            }
            container.addView(row)
        }
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

    // ---- 内置插件 ----

    private fun renderBuiltin(rows: List<BuiltinPluginManager.Row>) {
        renderGrid(binding?.builtinContainer, rows) { card, row ->
            card.name.text = row.name
            card.meta.text = if (row.installed) "内置插件" else "已停用"
            card.action.text = getString(com.stupidtree.hitax.R.string.plugin_builtin_uninstall)
            card.action.isEnabled = row.installed
            card.action.setOnClickListener { confirmUninstallBuiltin(row) }
            bindToggle(card, row.installed) { enabled ->
                viewModel.setBuiltinEnabled(row, enabled)
            }
        }
    }

    private fun confirmUninstallBuiltin(row: BuiltinPluginManager.Row) {
        AlertDialog.Builder(requireContext())
            .setTitle(com.stupidtree.hitax.R.string.plugin_uninstall_title)
            .setMessage(getString(com.stupidtree.hitax.R.string.plugin_uninstall_message, row.name))
            .setPositiveButton(com.stupidtree.hitax.R.string.plugin_uninstall_keep) { _, _ ->
                viewModel.uninstallBuiltin(row, purgeData = false)
            }
            .setNegativeButton(com.stupidtree.hitax.R.string.plugin_uninstall_clear) { _, _ ->
                viewModel.uninstallBuiltin(row, purgeData = true)
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    // ---- 界面插件 ----

    private var latestUiInstalled: List<InstalledUiPlugin> = emptyList()
    private var latestUiAvailable: List<UiMarketPlugin> = emptyList()

    private sealed class UiEntry {
        data class Available(val plugin: UiMarketPlugin, val update: Boolean) : UiEntry()
        data class Installed(val plugin: InstalledUiPlugin) : UiEntry()
    }

    private fun renderUiRows(installed: List<InstalledUiPlugin>, available: List<UiMarketPlugin>) {
        latestUiInstalled = installed
        latestUiAvailable = available
        val installedById = installed.associateBy { it.id }
        val pending = available
            .filter { av -> installedById[av.id]?.version != av.version }
            .map { UiEntry.Available(it, installedById.containsKey(it.id)) }
        val entries: List<UiEntry> = pending + installed.map { UiEntry.Installed(it) }
        binding?.uiEmpty?.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        val indexBase = com.stupidtree.hitax.BuildConfig.PLUGIN_INDEX_URL.takeIf { it.isNotBlank() }

        renderGrid(binding?.uiContainer, entries) { card, entry ->
            when (entry) {
                is UiEntry.Available -> {
                    val av = entry.plugin
                    card.name.text = av.name
                    card.meta.text = "v${av.version}"
                    card.action.text = getString(
                        if (entry.update) com.stupidtree.hitax.R.string.plugin_ui_update
                        else com.stupidtree.hitax.R.string.plugin_ui_install_action
                    )
                    card.action.isEnabled = true
                    card.toggle.visibility = View.INVISIBLE
                    card.action.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setMessage(com.stupidtree.hitax.R.string.plugin_ui_warning)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.installUiPluginFromUrl(av, indexBase)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
                is UiEntry.Installed -> {
                    val plugin = entry.plugin
                    card.name.text = plugin.name
                    card.meta.text = "v${plugin.version}"
                    card.action.text = getString(com.stupidtree.hitax.R.string.plugin_builtin_uninstall)
                    card.action.isEnabled = true
                    card.action.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle(com.stupidtree.hitax.R.string.plugin_builtin_uninstall)
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
            }
        }
    }

    // ---- 在线逻辑插件 ----

    private fun renderMarket(items: List<PluginMarketViewModel.Item>) {
        binding?.emptyHint?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        renderGrid(binding?.container, items) { card, item ->
            card.name.text = item.entry.name
            card.meta.text = "v${item.entry.version} · api ${item.entry.apiVersion}"
            card.action.text = if (item.installed) "卸载" else "安装"
            card.action.isEnabled = true
            card.action.setOnClickListener {
                when {
                    item.installed -> viewModel.uninstall(item.entry)
                    viewModel.isVerified(item.entry.id) -> viewModel.install(item.entry)
                    else -> confirmUntrusted(item.entry.name) { viewModel.install(item.entry) }
                }
            }
            if (item.installed) {
                card.toggle.visibility = View.VISIBLE
                bindToggle(card, true) { enabled ->
                    viewModel.setMarketEnabled(item.entry, enabled)
                }
            } else {
                card.toggle.visibility = View.INVISIBLE
            }
        }
    }

    // ---- 独立 APK 插件 ----

    private fun renderApkPlugins(plugins: List<ApkPlugin>) {
        binding?.apkEmptyHint?.visibility = if (plugins.isEmpty()) View.VISIBLE else View.GONE
        renderGrid(binding?.apkContainer, plugins) { card, plugin ->
            card.name.text = plugin.name
            card.meta.text = "v${plugin.versionName} · api ${plugin.apiVersion}"
            card.toggle.visibility = View.INVISIBLE
            val supported = viewModel.supported(plugin)
            card.action.text = if (supported) "打开" else "版本不兼容"
            card.action.isEnabled = supported
            card.action.setOnClickListener {
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
    }

    private fun confirmUntrusted(name: String, action: () -> Unit) {
        com.stupidtree.style.widgets.PopUpText()
            .setTitle(com.stupidtree.hitax.R.string.plugin_untrusted_title)
            .setText(getString(com.stupidtree.hitax.R.string.plugin_untrusted_message, name))
            .setOnConfirmListener(object : com.stupidtree.style.widgets.PopUpText.OnConfirmListener {
                override fun OnConfirm() = action()
            })
            .show(parentFragmentManager, "plugin_untrusted")
    }
}
