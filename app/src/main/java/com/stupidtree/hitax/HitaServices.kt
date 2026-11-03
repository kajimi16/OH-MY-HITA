package com.stupidtree.hitax

import android.content.Context
import com.stupidtree.hita.api.TimetableService

/**
 * 宿主取插件服务的唯一入口。
 *
 * 插件缺席时直接抛错（属于装配错误，应该尽早暴露），
 * 而不是静默返回 null 让调用方各自兜底。
 */
object HitaServices {

    fun timetable(context: Context): TimetableService =
        (context.applicationContext as HApplication).pluginHost
            .service(TimetableService.KEY)
            ?: error("timetable plugin is not installed")
}
