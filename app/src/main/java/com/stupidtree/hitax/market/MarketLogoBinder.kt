package com.stupidtree.hitax.market

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.stupidtree.hitax.R

/**
 * 统一绑定插件图标：优先加载索引 logo，缺失/失败时显示首字占位。
 */
object MarketLogoBinder {

    private val colors = listOf("#5B8DEF", "#E48A5B", "#7C6AE6", "#3FAE8A", "#D06A9E", "#B08A45")

    fun bind(
        image: ImageView,
        monogram: TextView,
        name: String,
        logo: String?,
        indexBase: String?,
    ) {
        val color = Color.parseColor(colors[Math.floorMod(name.hashCode(), colors.size)])
        monogram.text = name.trim().take(1).ifBlank { "?" }.uppercase()
        monogram.setBackgroundResource(R.drawable.bg_market_logo)
        monogram.backgroundTintList = ColorStateList.valueOf(color)
        monogram.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        image.contentDescription = image.context.getString(R.string.market_logo_content_description, name)
        image.visibility = View.GONE
        Glide.with(image).clear(image)
        image.setImageDrawable(null)
        if (logo.isNullOrBlank()) return
        val resolved = runCatching { PluginUrls.resolve(logo, indexBase) }.getOrNull() ?: return
        image.visibility = View.VISIBLE
        Glide.with(image)
            .load(resolved)
            .centerCrop()
            .into(image)
    }
}
