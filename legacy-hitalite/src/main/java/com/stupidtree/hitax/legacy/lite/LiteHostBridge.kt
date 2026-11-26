package com.stupidtree.hitax.legacy.lite

import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.EasSessionService
import com.stupidtree.hita.api.EasUiService
import com.stupidtree.hita.api.EmptyClassroomService
import com.stupidtree.hita.api.ExamService
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hita.api.ScoreService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.TimetableUiService
import com.stupidtree.hita.api.TeacherService
import com.stupidtree.hita.api.UserProfileService

/**
 * 宿主服务的只读入口。
 *
 * legacy 模块不依赖 app/kernel，由 PresetShellRouter 在启动 Lite shell 前注入。
 * 页面只通过这里取服务，禁止再建 Room/Retrofit/登录单例。
 */
object LiteHostBridge {
    @Volatile var timetable: TimetableService? = null
    @Volatile var timetableUi: TimetableUiService? = null
    @Volatile var teacher: TeacherService? = null
    @Volatile var eas: EasService? = null
    @Volatile var easAccount: EasAccountService? = null
    @Volatile var easSession: EasSessionService? = null
    @Volatile var easUi: EasUiService? = null
    @Volatile var easAuth: EasAuthService? = null
    @Volatile var scores: ScoreService? = null
    @Volatile var exams: ExamService? = null
    @Volatile var classrooms: EmptyClassroomService? = null
    @Volatile var userProfile: UserProfileService? = null
    @Volatile var resourceUi: ResourceUiService? = null
    @Volatile var openAgreement: ((androidx.fragment.app.FragmentManager) -> Unit)? = null
    @Volatile var openAbout: ((android.app.Activity) -> Unit)? = null
    @Volatile var openAddEvent: ((android.app.Activity) -> Unit)? = null
    @Volatile var openTimetableManager: ((android.app.Activity) -> Unit)? = null
    @Volatile var openTimetableDetail: ((android.app.Activity, String) -> Unit)? = null
    @Volatile var openSearch: ((android.app.Activity) -> Unit)? = null
    @Volatile var openImport: ((android.app.Activity) -> Unit)? = null
    @Volatile var openClassroom: ((android.app.Activity) -> Unit)? = null
    @Volatile var openScores: ((android.app.Activity) -> Unit)? = null
    @Volatile var openExams: ((android.app.Activity) -> Unit)? = null
    @Volatile var openLogin: ((android.app.Activity) -> Unit)? = null
    @Volatile var openMarket: ((android.app.Activity) -> Unit)? = null

    fun clear() {
        timetable = null
        timetableUi = null
        teacher = null
        eas = null
        easAccount = null
        easSession = null
        easUi = null
        easAuth = null
        scores = null
        exams = null
        classrooms = null
        userProfile = null
        resourceUi = null
        openAgreement = null
        openAbout = null
        openAddEvent = null
        openTimetableManager = null
        openTimetableDetail = null
        openSearch = null
        openImport = null
        openClassroom = null
        openScores = null
        openExams = null
        openLogin = null
        openMarket = null
    }
}
