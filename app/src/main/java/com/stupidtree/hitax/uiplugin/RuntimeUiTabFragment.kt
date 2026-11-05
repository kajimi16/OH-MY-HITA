package com.stupidtree.hitax.uiplugin

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.RuntimeUiService
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hitax.HApplication

/**
 * 宿主主界面的固定第五槽位：把运行时界面插件的 Compose 视图直接嵌进 ViewPager。
 *
 * Fragment 本身属于宿主，类名稳定，不依赖插件 dex；插件只提供 View。
 */
class RuntimeUiTabFragment : Fragment() {

    private var plugin: ComposeUiPlugin? = null
    private val args = Bundle()

    private val capability: String
        get() = arguments?.getString(ARG_CAPABILITY)
            ?: com.stupidtree.hita.api.RuntimeUiService.CAP_MAIN_TAB

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val manager = UiPluginManager(context)
        args.putString("tabId", arguments?.getString(ARG_TAB_ID).orEmpty())
        args.putString("tabTitle", arguments?.getString(ARG_TAB_TITLE).orEmpty())
        args.putString("capability", capability)
        val record = manager.resolveForCapability(capability)
            ?: return message(context, "插件未安装或已停用")
        return try {
            val instance = manager.instantiate(record)
            plugin = instance
            instance.createView(context, host)
        } catch (t: Throwable) {
            Log.e(TAG, "load runtime tab plugin failed", t)
            manager.setEnabled(record.id, false)
            message(context, "插件加载失败，已停用：${t.message}")
        }
    }

    private val host = object : PluginUiHost {
        override fun <T : Any> service(key: ServiceKey<T>): T? =
            (requireContext().applicationContext as HApplication).pluginHost.service(key)

        override val arguments: Bundle get() = args

        override fun close() {
            // 原型：关闭按钮回到第一个 tab。
            activity?.findViewById<androidx.viewpager.widget.ViewPager>(com.stupidtree.hitax.R.id.pager)
                ?.setCurrentItem(0, true)
        }
    }

    private fun message(context: Context, text: String): View =
        TextView(context).apply {
            this.text = text
            setPadding(48, 96, 48, 48)
        }

    override fun onDestroy() {
        runCatching { plugin?.onDestroy() }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RuntimeUiTab"
        private const val ARG_CAPABILITY = "capability"
        private const val ARG_TAB_ID = "tabId"
        private const val ARG_TAB_TITLE = "tabTitle"

        fun newInstance(spec: com.stupidtree.hita.api.MainTabSpec): RuntimeUiTabFragment =
            RuntimeUiTabFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CAPABILITY, spec.capability)
                    putString(ARG_TAB_ID, spec.id)
                    putString(ARG_TAB_TITLE, spec.title)
                }
            }
    }
}
