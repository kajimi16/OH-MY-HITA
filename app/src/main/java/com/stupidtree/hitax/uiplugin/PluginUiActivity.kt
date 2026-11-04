package com.stupidtree.hitax.uiplugin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hitax.HApplication

/**
 * 运行时界面插件的宿主容器。
 *
 * 只做三件事：加载插件、把插件返回的 View 设为内容、转发生命周期。
 * 插件自身没有 Activity/资源表，所以不会出现在系统的组件列表里。
 */
class PluginUiActivity : AppCompatActivity() {

    private var plugin: ComposeUiPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_PLUGIN_ID) ?: run {
            finish(); return
        }
        val record = UiPluginStore.find(this, id) ?: run {
            Toast.makeText(this, "插件未安装", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        if (UiPluginStore.isDisabled(this, id)) {
            Toast.makeText(this, "插件已停用", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        try {
            val instance = UiPluginManager(this).instantiate(record)
            plugin = instance
            setContentView(instance.createView(this, host))
        } catch (t: Throwable) {
            Log.e(TAG, "load ui plugin failed: $id", t)
            // 坏插件不应反复拖垮入口：停用后下次直接回退内置页面。
            UiPluginStore.setEnabled(this, id, false)
            Toast.makeText(this, "插件加载失败，已停用：${t.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val args: android.os.Bundle by lazy {
        android.os.Bundle().apply {
            intent?.getBundleExtra(EXTRA_ARGUMENTS)?.let { putAll(it) }
        }
    }

    private val host = object : PluginUiHost {
        override fun <T : Any> service(key: ServiceKey<T>): T? =
            (application as HApplication).pluginHost.service(key)

        override val arguments: android.os.Bundle get() = args

        override fun close() {
            finish()
        }
    }

    override fun onDestroy() {
        runCatching { plugin?.onDestroy() }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PLUGIN_ID = "pluginId"
        /** 调用方传参统一放在这个 Bundle 里，避免与宿主内部 extra 冲突。 */
        const val EXTRA_ARGUMENTS = "pluginArguments"
        private const val TAG = "PluginUiActivity"
    }
}
