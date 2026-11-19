package com.stupidtree.hitax.market

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.ItemMarketSourceBinding
import com.stupidtree.hitax.databinding.SheetMarketSourcesBinding

/**
 * 插件源管理面板。默认源不可删除；选择源后只回调，刷新由市场 Fragment 负责。
 */
class MarketSourcesSheet(
    context: Context,
    private val sources: List<MarketSource>,
    private val active: MarketSource,
    private val status: String?,
    private val listener: Listener,
) : BottomSheetDialog(context) {

    interface Listener {
        fun onSelect(source: MarketSource)
        fun onRemove(source: MarketSource)
        fun onAdd()
    }

    private val binding = SheetMarketSourcesBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.sourceToolbar.setNavigationOnClickListener { dismiss() }
        binding.sourceStatus.text = buildString {
            append(context.getString(R.string.market_source_status, active.name))
            status?.takeIf { it.isNotBlank() }?.let {
                append(" · ")
                append(it)
            }
        }
        sources.forEachIndexed { index, source ->
            val row = ItemMarketSourceBinding.inflate(
                LayoutInflater.from(context),
                binding.sourceContainer,
                false,
            )
            row.sourceName.text = source.name
            row.sourceUrl.text = source.url.ifBlank {
                context.getString(R.string.market_source_internal)
            }
            row.sourceCurrent.visibility = if (source == active) android.view.View.VISIBLE else android.view.View.GONE
            row.sourceRemove.visibility = if (index > 0) android.view.View.VISIBLE else android.view.View.GONE
            row.sourceRoot.setOnClickListener {
                dismiss()
                listener.onSelect(source)
            }
            row.sourceRemove.setOnClickListener {
                dismiss()
                listener.onRemove(source)
            }
            binding.sourceContainer.addView(row.root)
        }
        binding.sourceAdd.setOnClickListener {
            dismiss()
            listener.onAdd()
        }
        setOnShowListener {
            behavior.isFitToContents = false
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }
}
