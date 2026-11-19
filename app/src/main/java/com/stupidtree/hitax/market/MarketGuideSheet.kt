package com.stupidtree.hitax.market

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stupidtree.hitax.databinding.SheetMarketGuideBinding

/**
 * 自定义插件指北。入口在工具栏更多菜单，不再占用一级 tab。
 */
class MarketGuideSheet(context: Context) : BottomSheetDialog(context) {

    private val binding = SheetMarketGuideBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.guideToolbar.setNavigationOnClickListener { dismiss() }
        binding.guideTemplate.setOnClickListener {
            open("https://github.com/kajimi16/hita-plugin-template")
        }
        binding.guideCommunity.setOnClickListener {
            open("https://github.com/kajimi16/hita-community-plugins")
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

    private fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
