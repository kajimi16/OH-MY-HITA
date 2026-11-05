package com.stupidtree.hitax.ui.main

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * 支持任意数量条目的底栏。
 *
 * Material 的 BottomNavigationView 最多 5 项，而主界面固定 4 页 + N 个插件页，
 * 因此这里用普通 LinearLayout 自绘：等分宽度、图标 + 文字、选中着色。
 */
class DynamicBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    data class Item(
        val id: Int,
        val title: String,
        val iconRes: Int,
        val icon: android.graphics.drawable.Drawable? = null,
    )

    var onItemSelected: ((Int) -> Boolean)? = null

    private var items: List<Item> = emptyList()
    private var selectedIndex = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(64)
        setBackgroundColor(colorAttr(com.stupidtree.style.R.attr.backgroundColorBottom))
    }

    fun submit(list: List<Item>, selected: Int = 0) {
        items = list
        selectedIndex = selected.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        removeAllViews()
        list.forEachIndexed { index, item ->
            addView(buildItem(item, index), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        applySelection()
    }

    fun select(index: Int, notify: Boolean = false) {
        if (index !in items.indices) return
        selectedIndex = index
        applySelection()
        if (notify) onItemSelected?.invoke(index)
    }

    private fun buildItem(item: Item, index: Int): View {
        val column = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { select(index, notify = true) }
            val ripple = TypedValue()
            if (context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, ripple, true
                )
            ) {
                background = ripple.resourceId.takeIf { it != 0 }
                    ?.let { ContextCompat.getDrawable(context, it) }
            }
        }
        ImageView(context).apply {
            if (item.icon != null) {
                setImageDrawable(item.icon)
                tag = "customIcon"
            } else {
                setImageResource(item.iconRes)
                tag = "icon"
            }
            layoutParams = LayoutParams(dp(21), dp(21)).apply { topMargin = dp(6) }
        }.also { column.addView(it) }
        TextView(context).apply {
            text = item.title
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            tag = "title"
        }.also { column.addView(it) }
        return column
    }

    private fun applySelection() {
        val active = colorAttr(com.stupidtree.style.R.attr.colorPrimary)
        val inactive = colorAttr(com.stupidtree.style.R.attr.colorPrimaryDisabled)
        for (index in 0 until childCount) {
            val column = getChildAt(index) as? LinearLayout ?: continue
            val icon = column.findViewWithTag<ImageView>("icon")
                ?: column.findViewWithTag<ImageView>("customIcon")
            val title = column.findViewWithTag<TextView>("title")
            val color = if (index == selectedIndex) active else inactive
            if (column.findViewWithTag<ImageView>("icon") != null) icon?.setColorFilter(color)
            title?.setTextColor(color)
            column.alpha = if (index == selectedIndex) 1f else 0.85f
        }
    }

    private fun colorAttr(attr: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId)
            else value.data
        } else {
            Color.GRAY
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
