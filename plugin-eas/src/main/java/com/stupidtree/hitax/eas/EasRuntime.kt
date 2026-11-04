package com.stupidtree.hitax.eas

import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService

/** 插件内运行时服务访问点，install 时注入。 */
object EasRuntime {
    @Volatile var timetable: TimetableService? = null
    @Volatile var hostUi: HostUiService? = null

    fun timetableOrThrow(): TimetableService =
        timetable ?: error("timetable service not installed")
}
