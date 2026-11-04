package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.TabContribution
import com.stupidtree.hita.api.TimetableUiService
import com.stupidtree.hitax.data.AppDatabase
import com.stupidtree.hitax.data.repository.SubjectRepository
import com.stupidtree.hitax.data.repository.TimetableRepository

/**
 * 课表插件。
 *
 * 负责两件事：
 * 1. 提供 [TimetableService] —— 宿主 UI / Worker / Widget 只通过它访问课表；
 * 2. 注册可同步的三张表（timetable / subject / event）。
 *
 * 数据库、DAO、仓库、ICS 解析都在本模块内，宿主看不到实现。
 */
class HitaPlugin(private val application: Application) : Plugin {

    override val id = "com.stupidtree.hitax.timetable"

    override val displayName = "课表"

    companion object {
        /** 主界面第 2 个槽位（时间表） */
        const val TAB_TIMETABLE = "hita.timetable"
        /** 主界面第 1 个槽位（今日） */
        const val TAB_TIMELINE = "hita.timeline"
    }

    /** 用户选择「卸载并清除数据」：课表库、样式偏好、插件私有目录。 */
    override fun purgeData() {
        AppDatabase.reset()
        TimetableRepository.resetInstance()
        SubjectRepository.resetInstance()
        if (!application.deleteDatabase("hita")) {
            // 仍有连接时 deleteDatabase 会返回 false，兜底直接删文件
            val path = application.getDatabasePath("hita")
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                java.io.File(path.path + suffix).delete()
            }
        }
        application.getSharedPreferences("timetable_style", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        java.io.File(application.filesDir, "plugins/$id").deleteRecursively()
    }

    override fun install(ctx: PluginContext) {
        val database = AppDatabase.getDatabase(application)

        val service = TimetableServiceImpl(
            timetables = TimetableRepository.getInstance(application),
            subjects = SubjectRepository.getInstance(application),
            database = database,
        )
        ctx.provide(TimetableService.KEY, service)
        ctx.provide(TimetableUiService.KEY, TimetableUiImpl())

        // 插件内部界面代码（Activity/ViewModel）没有 PluginContext，通过运行时单例取用
        HitaRuntime.timetable = service
        HitaRuntime.hostUi = ctx.service(HostUiService.KEY)

        ctx.tabs.register(
            TabContribution(
                id = TAB_TIMELINE,
                titleRes = 0,
                iconRes = 0,
                order = 5,
            ) { com.stupidtree.hitax.ui.main.timeline.FragmentTimeLine() }
        )
        ctx.tabs.register(
            TabContribution(
                id = TAB_TIMETABLE,
                titleRes = 0,
                iconRes = 0,
                order = 10,
            ) { com.stupidtree.hitax.ui.main.timetable.TimetableFragment() }
        )

        ctx.sync.register(TimetableCodec(database.timetableDao()))
        ctx.sync.register(SubjectCodec(database.subjectDao()))
        ctx.sync.register(EventCodec(database.eventItemDao()))
    }
}
