package com.stupidtree.hitax.shell

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.market.PluginMarketActivity
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
        val easService = host?.service(EasService.KEY)
        setContent {
            CakeniReplicaRoot(
                timetableService = timetable,
                todoService = todo,
                hostUi = hostUi,
                easAuth = easAuth,
                easService = easService,
                onOpenMarket = { startActivity(Intent(this, PluginMarketActivity::class.java)) },
                onImport = {
                    easService?.openImportTimetable(this, autoImport = true)
                        ?: hostUi?.openImportTimetable(this)
                },
                onWebLogin = {
                    webLogin.launch(Intent(this, ShellEasWebLoginActivity::class.java))
                },
                onAddCourse = { table -> hostUi?.openAddEvent(this, table) },
                onClose = { finish() },
            )
        }
    }
}
