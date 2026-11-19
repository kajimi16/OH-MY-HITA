package com.stupidtree.hitax.market

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.SheetMarketFiltersBinding

/**
 * 筛选面板。筛选值只在“应用”时回调，取消不会改变当前列表。
 */
class MarketFilterSheet(
    context: Context,
    private val tab: MarketTab,
    private val current: MarketFilters,
    private val onApply: (MarketFilters) -> Unit,
) : BottomSheetDialog(context) {

    private val binding = SheetMarketFiltersBinding.inflate(layoutInflater)
    private var kind = current.normalized(tab).kind
    private var status = current.normalized(tab).status

    init {
        setContentView(binding.root)
        renderChips()
        binding.filterReset.setOnClickListener {
            kind = MarketKind.ALL
            status = MarketStatus.ALL
            renderChips()
        }
        binding.filterApply.setOnClickListener {
            dismiss()
            onApply(MarketFilters(kind, status).normalized(tab))
        }
    }

    private fun renderChips() {
        binding.typeGroup.removeAllViews()
        typeOptions().forEach { option ->
            addChip(binding.typeGroup, option.first, typeLabel(option.first), option.first == kind) {
                kind = option.first
                renderChips()
            }
        }
        binding.statusGroup.removeAllViews()
        statusOptions().forEach { option ->
            addChip(binding.statusGroup, option.first, statusLabel(option.first), option.first == status) {
                status = option.first
                renderChips()
            }
        }
    }

    private fun addChip(
        group: ChipGroup,
        value: Enum<*>,
        label: String,
        checked: Boolean,
        onClick: () -> Unit,
    ) {
        val chip = Chip(context).apply {
            text = label
            isCheckable = true
            isChecked = checked
            textSize = 13f
            minHeight = dp(36)
            setOnClickListener { onClick() }
        }
        group.addView(chip)
    }

    private fun typeOptions(): List<Pair<MarketKind, Int>> = when (tab) {
        MarketTab.EXPLORE -> listOf(
            MarketKind.ALL to 0,
            MarketKind.PRESET to 0,
            MarketKind.UI to 0,
            MarketKind.DEX to 0,
        )
        MarketTab.INSTALLED -> listOf(
            MarketKind.ALL to 0,
            MarketKind.UI to 0,
            MarketKind.DEX to 0,
            MarketKind.BUILTIN to 0,
            MarketKind.APK to 0,
        )
        MarketTab.UPDATES -> listOf(MarketKind.ALL to 0, MarketKind.UI to 0, MarketKind.DEX to 0)
    }

    private fun statusOptions(): List<Pair<MarketStatus, Int>> = when (tab) {
        MarketTab.EXPLORE -> listOf(
            MarketStatus.ALL to 0,
            MarketStatus.AVAILABLE to 0,
            MarketStatus.INSTALLED to 0,
            MarketStatus.UPDATES to 0,
        )
        MarketTab.INSTALLED -> listOf(
            MarketStatus.ALL to 0,
            MarketStatus.ENABLED to 0,
            MarketStatus.DISABLED to 0,
            MarketStatus.UPDATES to 0,
        )
        MarketTab.UPDATES -> listOf(MarketStatus.ALL to 0)
    }

    private fun typeLabel(kind: MarketKind): String = when (kind) {
        MarketKind.ALL -> context.getString(R.string.market_filter_all)
        MarketKind.PRESET -> context.getString(R.string.market_kind_preset)
        MarketKind.UI -> context.getString(R.string.market_kind_ui)
        MarketKind.DEX -> context.getString(R.string.market_kind_dex)
        MarketKind.BUILTIN -> context.getString(R.string.market_kind_builtin)
        MarketKind.APK -> context.getString(R.string.market_kind_apk)
    }

    private fun statusLabel(status: MarketStatus): String = when (status) {
        MarketStatus.ALL -> context.getString(R.string.market_filter_all)
        MarketStatus.AVAILABLE -> context.getString(R.string.market_status_available)
        MarketStatus.INSTALLED -> context.getString(R.string.market_status_installed)
        MarketStatus.UPDATES -> context.getString(R.string.market_status_update)
        MarketStatus.ENABLED -> context.getString(R.string.market_status_enabled)
        MarketStatus.DISABLED -> context.getString(R.string.market_status_disabled)
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
