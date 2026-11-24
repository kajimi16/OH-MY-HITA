package com.stupidtree.hitax.shell

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.market.PluginMarketActivity
import com.stupidtree.hitax.replica.cakeni.CakeniReplicaRoot

class CakeniReplicaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val host = (application as? HApplication)?.pluginHost
        val timetable = host?.service(TimetableService.KEY)
        val todo = host?.service(TodoService.KEY)
        val hostUi = host?.service(HostUiService.KEY)
        setContent {
            CakeniReplicaRoot(
                timetableService = timetable,
                todoService = todo,
                hostUi = hostUi,
                onOpenMarket = { startActivity(Intent(this, PluginMarketActivity::class.java)) },
                onImport = { hostUi?.openImportTimetable(this) },
                onAddCourse = { table -> hostUi?.openAddEvent(this, table) },
                onClose = { finish() },
            )
        }
    }
}
