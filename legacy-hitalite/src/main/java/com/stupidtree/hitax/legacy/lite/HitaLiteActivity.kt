package com.stupidtree.hitax.legacy.lite

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * HITA_Lite legacy shell.
 *
 * 阶段 1 只建立独立宿主壳；后续页面从 HITA_Lite 源码机械迁入本模块。
 * 数据、登录、数据库和组件注册一律不迁入，统一走宿主服务。
 */
class HitaLiteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        root.addView(TextView(this).apply {
            text = "HITA-Lite"
            textSize = 24f
        })
        setContentView(root)
    }
}
