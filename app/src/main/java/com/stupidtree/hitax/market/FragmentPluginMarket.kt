package com.stupidtree.hitax.market

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stupidtree.hitax.databinding.FragmentPluginMarketBinding
import com.stupidtree.hitax.databinding.ItemMarketPluginCardBinding
import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.plugin.PluginPrefs
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import com.stupidtree.style.base.BaseFragment

/**
 * 插件市场：只分「内置插件」与「市场插件」两个分区。
 *
 * - 内置插件：随 APK 编译，不能卸载，只能停用/启用。
 * - 市场插件：运行时安装（界面插件 / 在线逻辑插件 / 独立 APK 插件），
 *   右侧为「卸载」按钮与启用开关。
 */
class FragmentPluginMarket : BaseFragment<PluginMarketViewModel, FragmentPluginMarketBinding>() {

    override fun getViewModelClass(): Class<PluginMarketViewModel> = PluginMarketViewModel::class.java

    override fun initViewBinding(): FragmentPluginMarketBinding =
        FragmentPluginMarketBinding.inflate(layoutInflater)


    override fun initViews(view: View) {
        viewModel.builtinPlugins.observe(this) { renderBuiltin(it) }
        viewModel.items.observe(this) { latestDex = it; renderMarketAll() }
        viewModel.uiPlugins.observe(this) { latestUiInstalled = it; renderMarketAll() }
        viewModel.availableUiPlugins.observe(this) { latestUiAvailable = it; renderMarketAll() }
        viewModel.apkPlugins.observe(this) { latestApk = it; renderMarketAll() }

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
                row.addView(View(requireContext()), LinearLayout.LayoutParams(0, 1, 1f))
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
            card.meta.text = if (row.installed) "内置插件 · 已启用" else "内置插件 · 已停用"
            card.toggle.visibility = View.GONE
            card.action.visibility = View.VISIBLE
            card.action.text = if (row.installed) "停用" else "启用"
            card.action.setOnClickListener {
                viewModel.setBuiltinEnabled(row, !row.installed)
            }
        }
    }

    // ---- 市场插件（界面 / 在线 / 独立 APK） ----

    private sealed class MarketCard {
        data class UiInstalled(val plugin: InstalledUiPlugin) : MarketCard()
        data class UiAvailable(val plugin: UiMarketPlugin, val update: Boolean) : MarketCard()
        data class Dex(val item: PluginMarketViewModel.Item) : MarketCard()
        data class Apk(val plugin: ApkPlugin) : MarketCard()
    }

    private var latestUiInstalled: List<InstalledUiPlugin> = emptyList()
    private var latestUiAvailable: List<UiMarketPlugin> = emptyList()
    private var latestDex: List<PluginMarketViewModel.Item> = emptyList()
    private var latestApk: List<ApkPlugin> = emptyList()

    private fun renderMarketAll() {
        val installedById = latestUiInstalled.associateBy { it.id }
        val entries = mutableListOf<MarketCard>()
        latestUiInstalled.forEach { entries += MarketCard.UiInstalled(it) }
        latestUiAvailable
            .filter { av -> installedById[av.id]?.version != av.version }
            .forEach { entries += MarketCard.UiAvailable(it, installedById.containsKey(it.id)) }
        latestDex.forEach { entries += MarketCard.Dex(it) }
        latestApk.forEach { entries += MarketCard.Apk(it) }

        binding?.marketEmpty?.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        val indexBase = com.stupidtree.hitax.BuildConfig.PLUGIN_INDEX_URL.takeIf { it.isNotBlank() }
        renderGrid(binding?.marketContainer, entries) { card, entry ->
            when (entry) {
                is MarketCard.UiInstalled -> bindUiInstalled(card, entry.plugin)
                is MarketCard.UiAvailable -> bindUiAvailable(card, entry, indexBase)
                is MarketCard.Dex -> bindDex(card, entry.item)
                is MarketCard.Apk -> bindApk(card, entry.plugin)
            }
        }
    }

    private fun bindUiInstalled(card: ItemMarketPluginCardBinding, plugin: InstalledUiPlugin) {
        card.name.text = plugin.name
        card.meta.text = "界面插件 · v${plugin.version}"
        card.action.visibility = View.VISIBLE
        card.action.text = getString(com.stupidtree.hitax.R.string.plugin_builtin_uninstall)
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

    private fun bindUiAvailable(
        card: ItemMarketPluginCardBinding,
        entry: MarketCard.UiAvailable,
        indexBase: String?,
    ) {
        card.name.text = entry.plugin.name
        card.meta.text = "界面插件 · v${entry.plugin.version}" +
            if (entry.update) " · 可更新" else ""
        card.action.visibility = View.VISIBLE
        card.action.text = getString(
            if (entry.update) com.stupidtree.hitax.R.string.plugin_ui_update
            else com.stupidtree.hitax.R.string.plugin_ui_install_action
        )
        card.toggle.visibility = View.GONE
        card.action.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(com.stupidtree.hitax.R.string.plugin_ui_warning)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.installUiPluginFromUrl(entry.plugin, indexBase)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun bindDex(card: ItemMarketPluginCardBinding, item: PluginMarketViewModel.Item) {
        card.name.text = item.entry.name
        card.meta.text = "在线插件 · v${item.entry.version} · api ${item.entry.apiVersion}"
        card.action.visibility = View.VISIBLE
        card.action.text = if (item.installed) "卸载" else "安装"
        card.action.setOnClickListener {
            when {
                item.installed -> viewModel.uninstall(item.entry)
                viewModel.isVerified(item.entry.id) -> viewModel.install(item.entry)
                else -> confirmUntrusted(item.entry.name) { viewModel.install(item.entry) }
            }
        }
        if (item.installed) {
            card.toggle.visibility = View.VISIBLE
            bindToggle(card, item.enabled) { enabled ->
                viewModel.setMarketEnabled(item.entry, enabled)
            }
        } else {
            card.toggle.visibility = View.GONE
        }
    }

    private fun bindApk(card: ItemMarketPluginCardBinding, plugin: ApkPlugin) {
        val disabledKey = "apk:${plugin.packageName}"
        card.name.text = plugin.name
        card.meta.text = "独立 APK · v${plugin.versionName}"
        card.action.visibility = View.VISIBLE
        card.action.text = getString(com.stupidtree.hitax.R.string.plugin_builtin_uninstall)
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
