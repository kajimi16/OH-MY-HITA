package com.stupidtree.hitax.eas

import android.app.Application
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TimetableService

/** 教务插件：提供 [EasService]，并把依赖的宿主能力放进 [EasRuntime]。 */
class EasPlugin(private val application: Application) : Plugin {

    override val id = "com.stupidtree.hitax.eas"

    override val displayName = "教务"

    /** 用户选择「卸载并清除数据」：教务凭据、成绩提醒、考试备忘与本插件目录。 */
    override fun purgeData() {
        runCatching {
            com.stupidtree.hitax.data.work.ScoreReminderScheduler.cancel(application)
        }
        listOf("local_eas_token", "score_reminder", "exam_memo").forEach { name ->
            application.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
        java.io.File(application.filesDir, "plugins/$id").deleteRecursively()
    }

    override fun install(ctx: PluginContext) {
        EasRuntime.ctx = ctx
        ctx.provide(EasService.KEY, EasServiceImpl(application))
        ctx.provide(com.stupidtree.hita.api.ScoreService.KEY, ScoreServiceImpl(application))
        ctx.provide(
            com.stupidtree.hita.api.TrainingPlanService.KEY,
            TrainingPlanServiceImpl(application),
        )
        ctx.provide(com.stupidtree.hita.api.ExamService.KEY, object : com.stupidtree.hita.api.ExamService {
            override fun exams() = com.stupidtree.hitax.data.repository.EASRepository.getInstance(application).getExamInfo()
        })
    }
}
