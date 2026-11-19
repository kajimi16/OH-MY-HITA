package com.stupidtree.hitax

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hita.api.TimetableService
import java.lang.reflect.Proxy

/**
 * 宿主取插件服务的唯一入口。
 *
 * 插件缺席（例如空壳构建）时返回一个 no-op 实现：读接口给空 LiveData / 空列表，
 * 写接口什么都不做。这样所有调用点不必各自判空，也不会因为插件缺失崩溃。
 */
object HitaServices {

    @Volatile
    private var warned = false

    fun timetable(context: Context): TimetableService =
        (context.applicationContext as HApplication).pluginHost
            .service(TimetableService.KEY)
            ?: noopTimetable().also {
                if (!warned) {
                    warned = true
                    Log.w("HitaServices", "timetable plugin missing, using no-op service")
                }
            }

    fun resourceUi(context: Context): ResourceUiService? =
        (context.applicationContext as HApplication).pluginHost.service(ResourceUiService.KEY)

    /** 用动态代理生成 no-op 实现：新增接口方法时无需同步维护。 */
    private fun noopTimetable(): TimetableService {
        val handler = java.lang.reflect.InvocationHandler { _, method, _ ->
            when (method.returnType) {
                Void.TYPE, Unit::class.java -> Unit
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                Double::class.javaPrimitiveType -> 0.0
                LiveData::class.java -> MutableLiveData<Any?>()
                List::class.java -> emptyList<Any>()
                Set::class.java -> emptySet<Any>()
                else -> null
            }
        }
        return Proxy.newProxyInstance(
            TimetableService::class.java.classLoader,
            arrayOf(TimetableService::class.java),
            handler,
        ) as TimetableService
    }
}
