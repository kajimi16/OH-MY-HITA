package com.stupidtree.hitax.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stupidtree.hita.api.ViewSearchTab
import com.stupidtree.hitax.HApplication

/**
 * 运行时逻辑插件搜索 tab 的宿主壳。
 *
 * Fragment 类名固定，插件只提供 View 与查询回调，避免恢复时找不到插件类。
 */
class RuntimeSearchTabFragment : Fragment() {

    private var tab: ViewSearchTab? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = arguments?.getString(ARG_ID).orEmpty()
        val host = (requireContext().applicationContext as HApplication).pluginHost
        tab = host.searchTabs.viewEntries().firstOrNull { it.id == id }
        return tab?.let { it.createView(requireContext()) }
            ?: TextView(requireContext()).apply { text = "插件未安装或已停用" }
    }

    fun setQuery(text: String) {
        tab?.onQuery?.invoke(text)
    }

    companion object {
        private const val ARG_ID = "tabId"

        fun newInstance(id: String): RuntimeSearchTabFragment =
            RuntimeSearchTabFragment().apply {
                arguments = Bundle().apply { putString(ARG_ID, id) }
            }
    }
}
