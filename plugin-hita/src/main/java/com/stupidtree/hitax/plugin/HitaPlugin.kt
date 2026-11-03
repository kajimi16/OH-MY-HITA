package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TimetableService
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

    override fun install(ctx: PluginContext) {
        val database = AppDatabase.getDatabase(application)

        ctx.provide(
            TimetableService.KEY,
            TimetableServiceImpl(
                timetables = TimetableRepository.getInstance(application),
                subjects = SubjectRepository.getInstance(application),
            )
        )

        ctx.sync.register(TimetableCodec(database.timetableDao()))
        ctx.sync.register(SubjectCodec(database.subjectDao()))
        ctx.sync.register(EventCodec(database.eventItemDao()))
    }
}
