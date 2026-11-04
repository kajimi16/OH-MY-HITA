package com.stupidtree.hitax.ui.main

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stupidtree.hitax.plugin.PluginCatalog

/**
 * 插件被用户卸载后，主界面原位置显示的占位页。
 *
 * 不缩容底部导航（保留 4 个槽位），而是明确告诉用户「这里的功能已被卸载」，
 * 并提供一键跳回插件市场重新启用。
 */
class PluginUnavailableFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val pluginId = arguments?.getString(ARG_PLUGIN_ID).orEmpty()
        val name = (context.applicationContext as? com.stupidtree.hitax.HApplication)
            ?.let { PluginCatalog.entry(it, pluginId)?.displayName }
            ?: pluginId

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(
            TextView(context).apply {
                text = getString(com.stupidtree.hitax.R.string.plugin_unavailable_title, name)
                textSize = 17f
                gravity = Gravity.CENTER
            }
        )
        root.addView(
            TextView(context).apply {
                text = getString(com.stupidtree.hitax.R.string.plugin_unavailable_hint)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 32)
            }
        )
        root.addView(
            Button(context).apply {
                text = getString(com.stupidtree.hitax.R.string.plugin_unavailable_action)
                setOnClickListener {
                    (activity as? MainActivity)?.openPluginMarket()
                }
            }
        )
        return root
    }

    companion object {
        private const val ARG_PLUGIN_ID = "pluginId"

        fun newInstance(pluginId: String): PluginUnavailableFragment =
            PluginUnavailableFragment().apply {
                arguments = Bundle().apply { putString(ARG_PLUGIN_ID, pluginId) }
            }
    }
}
