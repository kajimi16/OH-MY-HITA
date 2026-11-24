package com.stupidtree.hitax.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.ItemMarketPluginBinding
import com.stupidtree.hitax.databinding.ItemMarketPresetBinding
import com.stupidtree.hitax.databinding.ItemMarketSectionBinding

/**
 * 单列市场列表。行数据是纯 [MarketItem]，Adapter 只做展示和回调分发。
 */
class MarketAdapter(
    private val listener: Listener,
) : ListAdapter<MarketItem, RecyclerView.ViewHolder>(DIFF) {

    interface Listener {
        fun onOpen(item: MarketItem)
        fun onAction(item: MarketItem)
        fun onToggle(item: MarketItem, checked: Boolean)
        fun onSectionAction(action: SectionAction)
    }

    var indexBase: String? = null

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MarketItem.Section -> TYPE_SECTION
        is MarketItem.Preset -> TYPE_PRESET
        else -> TYPE_PLUGIN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> SectionHolder(ItemMarketSectionBinding.inflate(inflater, parent, false))
            TYPE_PRESET -> PresetHolder(ItemMarketPresetBinding.inflate(inflater, parent, false))
            else -> PluginHolder(ItemMarketPluginBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionHolder -> bindSection(holder.binding, getItem(position) as MarketItem.Section)
            is PresetHolder -> bindPreset(holder.binding, getItem(position) as MarketItem.Preset)
            is PluginHolder -> bindPlugin(holder.binding, getItem(position))
        }
    }

    private fun bindSection(binding: ItemMarketSectionBinding, item: MarketItem.Section) {
        binding.sectionTitle.text = when (item.kind) {
            SectionKind.UPDATES -> binding.root.context.getString(R.string.market_updates_title)
            SectionKind.PRESETS -> binding.root.context.getString(R.string.market_section_presets)
            SectionKind.ALL_PLUGINS -> binding.root.context.getString(R.string.market_section_all)
            SectionKind.ENABLED -> binding.root.context.getString(R.string.market_section_enabled)
            SectionKind.DISABLED -> binding.root.context.getString(R.string.market_section_disabled)
        }
        binding.sectionCount.text = item.count.toString()
        binding.sectionCount.visibility = if (item.count > 0) View.VISIBLE else View.GONE
        binding.sectionSubtitle.text = item.subtitle.orEmpty()
        binding.sectionSubtitle.visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.sectionAction.visibility = if (item.action == null) View.GONE else View.VISIBLE
        if (item.action != null) {
            binding.sectionAction.text = binding.root.context.getString(
                when (item.action) {
                    SectionAction.UPDATE_ALL -> R.string.market_update_all
                    SectionAction.CLEAR_FILTERS -> R.string.market_clear_filters
                },
            )
            binding.sectionAction.setOnClickListener { listener.onSectionAction(item.action) }
        }
    }

    private fun bindPreset(binding: ItemMarketPresetBinding, item: MarketItem.Preset) {
        val preset = item.preset
        binding.presetName.text = preset.name
        binding.presetMeta.text = buildString {
            append(binding.root.context.getString(R.string.market_kind_preset))
            append(" · v")
            append(preset.version)
            append(" · ")
            append(preset.refs().size)
            append(" 个插件")
            if (item.active) append(" · ").append(binding.root.context.getString(R.string.market_status_active))
        }
        binding.presetDescription.text = preset.description
        binding.presetDescription.visibility =
            if (preset.description.isNullOrBlank()) View.GONE else View.VISIBLE
        MarketLogoBinder.bind(
            binding.presetLogo,
            binding.presetMonogram,
            preset.name,
            null,
            indexBase,
        )
        binding.presetAction.text = when {
            item.active -> binding.root.context.getString(R.string.market_status_active)
            else -> binding.root.context.getString(R.string.market_action_view)
        }
        binding.presetAction.isEnabled = !item.active
        binding.presetAction.setOnClickListener { listener.onOpen(item) }
        binding.root.setOnClickListener { listener.onOpen(item) }
    }

    private fun bindPlugin(binding: ItemMarketPluginBinding, item: MarketItem) {
        val context = binding.root.context
        var actionLabel: String? = null
        var actionEnabled = true
        var switchVisible = false
        var switchChecked = false
        var badge: String? = null
        var meta = ""
        var description: String? = null
        var logo: String? = null
        var name = ""

        when (item) {
            is MarketItem.Ui -> {
                name = item.plugin.name
                description = item.plugin.description
                logo = item.plugin.logo
                meta = context.getString(R.string.market_kind_ui) + " · v" + item.plugin.version
                when (item.tab) {
                    MarketTab.EXPLORE -> {
                        if (item.installed != null) {
                            actionLabel = if (item.update) context.getString(R.string.market_action_update) else null
                            badge = if (item.update) {
                                context.getString(R.string.market_status_update)
                            } else {
                                context.getString(R.string.market_status_installed)
                            }
                        } else {
                            actionLabel = context.getString(R.string.market_action_install)
                        }
                    }
                    MarketTab.INSTALLED -> {
                        if (item.update) actionLabel = context.getString(R.string.market_action_update)
                        switchVisible = true
                        switchChecked = item.installed?.enabled == true
                        if (item.installed?.enabled == false) badge = context.getString(R.string.market_status_disabled)
                        if (item.update && badge == null) badge = context.getString(R.string.market_status_update)
                    }
                    MarketTab.UPDATES -> {
                        actionLabel = context.getString(R.string.market_action_update)
                        badge = context.getString(R.string.market_status_update)
                    }
                }
            }
            is MarketItem.Dex -> {
                name = item.item.entry.name
                description = item.item.entry.description
                logo = item.item.entry.logo
                meta = context.getString(R.string.market_kind_dex) +
                    " · v" + item.item.entry.version +
                    " · API " + item.item.entry.apiVersion
                when (item.tab) {
                    MarketTab.EXPLORE -> {
                        if (item.item.installed) {
                            actionLabel = if (item.item.update) context.getString(R.string.market_action_update) else null
                            badge = if (item.item.update) {
                                context.getString(R.string.market_status_update)
                            } else {
                                context.getString(R.string.market_status_installed)
                            }
                        } else {
                            actionLabel = context.getString(R.string.market_action_install)
                        }
                    }
                    MarketTab.INSTALLED -> {
                        if (item.item.update) actionLabel = context.getString(R.string.market_action_update)
                        switchVisible = true
                        switchChecked = item.item.enabled
                        if (!item.item.enabled) badge = context.getString(R.string.market_status_disabled)
                        if (item.item.update && badge == null) badge = context.getString(R.string.market_status_update)
                    }
                    MarketTab.UPDATES -> {
                        actionLabel = context.getString(R.string.market_action_update)
                        badge = context.getString(R.string.market_status_update)
                    }
                }
            }
            is MarketItem.Builtin -> {
                name = item.row.name
                meta = context.getString(R.string.market_kind_builtin)
                switchVisible = true
                switchChecked = item.row.installed
                badge = if (item.row.installed) {
                    context.getString(R.string.market_section_enabled)
                } else {
                    context.getString(R.string.market_status_disabled)
                }
            }
            is MarketItem.Apk -> {
                name = item.plugin.name
                meta = context.getString(R.string.market_kind_apk) + " · v" + item.plugin.versionName
                switchVisible = true
                switchChecked = item.enabled
                if (!item.enabled) badge = context.getString(R.string.market_status_disabled)
            }
            is MarketItem.Preset, is MarketItem.Section -> return
        }

        binding.pluginName.text = name
        binding.pluginDescription.text = description.orEmpty()
        binding.pluginDescription.visibility = if (description.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.pluginMeta.text = meta
        binding.pluginBadge.text = badge.orEmpty()
        binding.pluginBadge.visibility = if (badge.isNullOrBlank()) View.GONE else View.VISIBLE
        MarketLogoBinder.bind(binding.pluginLogo, binding.pluginMonogram, name, logo, indexBase)

        binding.pluginAction.visibility = if (actionLabel == null) View.GONE else View.VISIBLE
        if (actionLabel != null) {
            binding.pluginAction.text = actionLabel
            binding.pluginAction.isEnabled = actionEnabled
            binding.pluginAction.setOnClickListener { listener.onAction(item) }
        }

        binding.pluginSwitch.visibility = if (switchVisible) View.VISIBLE else View.GONE
        if (switchVisible) {
            binding.pluginSwitch.setOnCheckedChangeListener(null)
            binding.pluginSwitch.isChecked = switchChecked
            binding.pluginSwitch.contentDescription = context.getString(
                R.string.market_switch_content_description,
                name,
            )
            binding.pluginSwitch.setOnCheckedChangeListener { _, checked ->
                listener.onToggle(item, checked)
            }
        }
        binding.root.setOnClickListener { listener.onOpen(item) }
    }

    private class SectionHolder(val binding: ItemMarketSectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class PresetHolder(val binding: ItemMarketPresetBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class PluginHolder(val binding: ItemMarketPluginBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_PRESET = 1
        private const val TYPE_PLUGIN = 2

        private val DIFF = object : DiffUtil.ItemCallback<MarketItem>() {
            override fun areItemsTheSame(oldItem: MarketItem, newItem: MarketItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MarketItem, newItem: MarketItem): Boolean =
                oldItem == newItem
        }
    }
}
