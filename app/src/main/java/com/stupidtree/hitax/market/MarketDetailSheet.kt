package com.stupidtree.hitax.market

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.SheetMarketDetailBinding

data class MarketDetailAction(
    val label: String,
    val onClick: () -> Unit,
)

data class MarketDetailModel(
    val title: String,
    val version: String?,
    val kind: String,
    val description: String?,
    val changelog: String?,
    val meta: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val presetPlugins: List<String> = emptyList(),
    val trustLabel: String? = null,
    val trustVerified: Boolean = false,
    val repo: String? = null,
    val logo: String? = null,
    val indexBase: String? = null,
    val primaryLabel: String? = null,
    val primaryEnabled: Boolean = true,
    val primary: (() -> Unit)? = null,
    val secondary: List<MarketDetailAction> = emptyList(),
    val versionOptions: List<String> = emptyList(),
    val selectedVersion: String? = null,
    val onVersionSelected: ((String) -> Unit)? = null,
)

/**
 * 全高详情面板。动作由调用方提供；面板只负责展示和关闭。
 */
class MarketDetailSheet(
    context: Context,
    private val model: MarketDetailModel,
) : BottomSheetDialog(context) {

    private val binding = SheetMarketDetailBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.detailToolbar.setNavigationOnClickListener { dismiss() }
        bind()
        setOnShowListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun bind() {
        val context = context
        binding.detailName.text = model.title
        binding.detailVersion.text = model.version?.takeIf { it.isNotBlank() }?.let {
            context.getString(R.string.market_detail_version, it)
        }
        binding.detailVersion.visibility = if (model.version.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.detailKind.text = model.kind
        binding.detailTrust.text = model.trustLabel.orEmpty()
        binding.detailTrust.visibility = if (model.trustLabel.isNullOrBlank()) View.GONE else View.VISIBLE

        val versionOptions = model.versionOptions.distinct()
        binding.detailVersionSelector.visibility =
            if (versionOptions.size > 1 && model.onVersionSelected != null) View.VISIBLE else View.GONE
        binding.detailVersionOptions.removeAllViews()
        versionOptions.forEach { version ->
            val selected = version == model.selectedVersion
            val chip = TextView(context).apply {
                text = context.getString(R.string.market_detail_version, version)
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), dp(7))
                setTextColor(
                    MaterialColors.getColor(
                        this,
                        if (selected) R.attr.textColorPrimary else R.attr.colorAccent,
                    ),
                )
                background = if (selected) {
                    val accent = MaterialColors.getColor(this, R.attr.colorAccent)
                    android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(
                            androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 0x33),
                        )
                    }
                } else {
                    ContextCompat.getDrawable(context, R.drawable.bg_market_chip)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (!selected) model.onVersionSelected?.invoke(version)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = dp(8) }
            binding.detailVersionOptions.addView(chip, params)
        }

        MarketLogoBinder.bind(
            binding.detailLogo,
            binding.detailMonogram,
            model.title,
            model.logo,
            model.indexBase,
        )

        binding.detailMeta.text = model.meta.joinToString("\n")
        binding.detailMeta.visibility = if (model.meta.isEmpty()) View.GONE else View.VISIBLE

        binding.detailDescription.text = model.description?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.market_detail_no_description)

        val changelog = model.changelog?.takeIf { it.isNotBlank() }
        binding.detailChangelogTitle.visibility = if (changelog == null) View.GONE else View.VISIBLE
        binding.detailChangelog.visibility = if (changelog == null) View.GONE else View.VISIBLE
        binding.detailChangelog.text = changelog.orEmpty()
        binding.detailTagsTitle.visibility = if (model.tags.isEmpty()) View.GONE else View.VISIBLE
        binding.detailTags.visibility = if (model.tags.isEmpty()) View.GONE else View.VISIBLE
        binding.detailTags.text = model.tags.joinToString(" · ")

        val hasPlugins = model.presetPlugins.isNotEmpty()
        binding.detailPluginsTitle.visibility = if (hasPlugins) View.VISIBLE else View.GONE
        binding.detailPlugins.visibility = if (hasPlugins) View.VISIBLE else View.GONE
        binding.detailPlugins.text = model.presetPlugins.joinToString("\n")

        val repo = model.repo?.takeIf { it.isNotBlank() }
        binding.detailRepo.visibility = if (repo == null) View.GONE else View.VISIBLE
        if (repo != null) {
            binding.detailRepo.setOnClickListener {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo)))
                }
            }
        }

        binding.detailSecondaryContainer.removeAllViews()
        model.secondary.forEach { action ->
            val button = TextView(context).apply {
                text = action.label
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setTextColor(MaterialColors.getColor(this, R.attr.colorAccent))
                background = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
                isClickable = true
                isFocusable = true
                minHeight = dp(48)
                setOnClickListener {
                    dismiss()
                    action.onClick()
                }
            }
            binding.detailSecondaryContainer.addView(
                button,
                android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val primary = model.primary
        binding.detailPrimary.visibility = if (primary == null) View.GONE else View.VISIBLE
        if (primary != null) {
            binding.detailPrimary.text = model.primaryLabel ?: context.getString(R.string.market_action_open)
            binding.detailPrimary.isEnabled = model.primaryEnabled
            binding.detailPrimary.setOnClickListener {
                if (model.primaryEnabled) {
                    dismiss()
                    primary.invoke()
                }
            }
        }
        binding.detailBottom.visibility =
            if (primary == null && model.secondary.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
