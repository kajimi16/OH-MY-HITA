package com.stupidtree.hitax.market

import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.stupidtree.hitax.databinding.FragmentPluginMarketBinding
import com.stupidtree.hitax.databinding.ItemPluginMarketBinding
import com.stupidtree.style.base.BaseFragment

/**
 * 「插件」主界面：与「今日」「时间表」「功能中心」并列。
 * 只上架逻辑插件（无界面、无资源），布局沿用应用的卡片风格。
 */
class FragmentPluginMarket : BaseFragment<PluginMarketViewModel, FragmentPluginMarketBinding>() {

    override fun getViewModelClass(): Class<PluginMarketViewModel> = PluginMarketViewModel::class.java

    override fun initViewBinding(): FragmentPluginMarketBinding =
        FragmentPluginMarketBinding.inflate(layoutInflater)

    override fun initViews(view: View) {
        viewModel.items.observe(this) { render(it) }
        viewModel.apkPlugins.observe(this) { renderApkPlugins(it) }
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
        // 从详情/外部返回时重新拉一次索引，避免看到过期列表
        viewModel.refresh()
    }

    private fun trustLabel(id: String): String = getString(
        if (viewModel.isVerified(id)) com.stupidtree.hitax.R.string.plugin_trust_verified
        else com.stupidtree.hitax.R.string.plugin_trust_unverified
    )

    /** 未验证插件在安装/打开前统一提示一次。 */
    private fun confirmUntrusted(name: String, action: () -> Unit) {
        com.stupidtree.style.widgets.PopUpText()
            .setTitle(com.stupidtree.hitax.R.string.plugin_untrusted_title)
            .setText(getString(com.stupidtree.hitax.R.string.plugin_untrusted_message, name))
            .setOnConfirmListener(object : com.stupidtree.style.widgets.PopUpText.OnConfirmListener {
                override fun OnConfirm() = action()
            })
            .show(parentFragmentManager, "plugin_untrusted")
    }

    private fun renderApkPlugins(plugins: List<ApkPlugin>) {
        val container = binding?.apkContainer ?: return
        binding?.apkEmptyHint?.visibility = if (plugins.isEmpty()) View.VISIBLE else View.GONE
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        plugins.forEach { plugin ->
            val row = com.stupidtree.hitax.databinding.ItemApkPluginBinding.inflate(inflater, container, false)
            row.apkName.text = plugin.name
            row.apkMeta.text = "${plugin.versionName} · ${plugin.packageName} · api ${plugin.apiVersion} · ${trustLabel(plugin.id)}"
            val supported = viewModel.supported(plugin)
            row.apkAction.text = if (supported) "打开" else "版本不兼容"
            row.apkAction.isEnabled = supported
            row.apkAction.setOnClickListener {
                val launch: () -> Unit = {
                    val intent = android.content.Intent()
                        .setComponent(plugin.entry)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { requireContext().startActivity(intent) }
                    Unit
                }
                if (viewModel.isVerified(plugin.id)) launch() else confirmUntrusted(plugin.name, launch)
            }
            container.addView(row.root)
        }
    }

    private fun render(items: List<PluginMarketViewModel.Item>) {
        val container = binding?.container ?: return
        binding?.emptyHint?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        items.forEach { item ->
            val row = ItemPluginMarketBinding.inflate(inflater, container, false)
            row.pluginName.text = item.entry.name
            row.pluginMeta.text = "${item.entry.version} · api ${item.entry.apiVersion} · ${trustLabel(item.entry.id)}"
            row.pluginId.text = item.entry.id
            row.pluginAction.text = if (item.installed) "卸载" else "安装"
            row.pluginAction.setOnClickListener {
                when {
                    item.installed -> viewModel.uninstall(item.entry)
                    viewModel.isVerified(item.entry.id) -> viewModel.install(item.entry)
                    else -> confirmUntrusted(item.entry.name) { viewModel.install(item.entry) }
                }
            }
            container.addView(row.root)
        }
    }
}
