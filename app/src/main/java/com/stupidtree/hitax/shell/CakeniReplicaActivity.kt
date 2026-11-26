package com.stupidtree.hitax.shell

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.market.PluginMarketActivity
import com.stupidtree.hitax.plugin.EasContracts
import com.stupidtree.hitax.replica.cakeni.CakeniReplicaRoot

class CakeniReplicaActivity : AppCompatActivity() {
    private val webLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val host = (application as? HApplication)?.pluginHost
        val timetable = host?.service(TimetableService.KEY)
        val todo = host?.service(TodoService.KEY)
        val hostUi = host?.service(HostUiService.KEY)
        val easAuth = host?.service(EasAuthService.KEY)
        val easSession = EasContracts.session(this)
        setContent {
            CakeniReplicaRoot(
                timetableService = timetable,
                todoService = todo,
                hostUi = hostUi,
                easAuth = easAuth,
                easSession = easSession,
                onOpenMarket = { startActivity(Intent(this, PluginMarketActivity::class.java)) },
                onImport = {
                    val service = easSession
                    if (service == null) {
                        android.widget.Toast.makeText(
                            this,
                            "教务插件未安装，无法导入",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            this,
                            "正在导入本学期课表…",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        // 数据导入走 EasSessionService，不启动插件 Activity（运行时 dex 无法注册 Activity）。
                        service.startAutoReimport { result ->
                            val ok = result.success
                            android.util.Log.i("CakeniImport", "startAutoReimport result=$ok")
                            runOnUiThread {
                                android.widget.Toast.makeText(
                                    this,
                                    if (ok) "课表导入完成" else "课表导入失败",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                                if (ok) recreate()
                            }
                        }
                    }
                },
                onWebLogin = {
                    webLogin.launch(Intent(this, ShellEasWebLoginActivity::class.java))
                },
                onApiLoginSuccess = { recreate() },
                onAddCourse = { table -> hostUi?.openAddEvent(this, table) },
                onClose = { finish() },
            )
        }
    }
}
