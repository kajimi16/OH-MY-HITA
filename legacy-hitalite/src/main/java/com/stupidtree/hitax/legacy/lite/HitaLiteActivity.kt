package com.stupidtree.hitax.legacy.lite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView

/**
 * HITA_Lite legacy shell.
 *
 * 阶段 2：先接入今日页；时间表/功能中心与底部导航随后接入。
 * 数据、登录、数据库和组件注册一律不迁入，统一走宿主服务。
 */
class HitaLiteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FragmentContainerView(this).apply {
            id = R.id.lite_shell_container
        }
        setContentView(container)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.lite_shell_container,
                    hostFragment("com.stupidtree.hitax.ui.main.timeline.FragmentTimeLine")
                        ?: Fragment(),
                )
                .commit()
        }
    }

    private fun hostFragment(className: String): Fragment? = runCatching {
        Class.forName(className, true, applicationContext.classLoader)
            .getDeclaredConstructor()
            .newInstance() as Fragment
    }.getOrNull()
}
