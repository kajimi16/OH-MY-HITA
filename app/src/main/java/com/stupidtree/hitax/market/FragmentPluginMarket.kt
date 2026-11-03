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
        viewModel.message.observe(this) { text ->
            text ?: return@observe
            Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
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
            row.pluginMeta.text = "${item.entry.version} · api ${item.entry.apiVersion}"
            row.pluginId.text = item.entry.id
            row.pluginAction.text = if (item.installed) "卸载" else "安装"
            row.pluginAction.setOnClickListener {
                if (item.installed) viewModel.uninstall(item.entry) else viewModel.install(item.entry)
            }
            container.addView(row.root)
        }
    }
}
