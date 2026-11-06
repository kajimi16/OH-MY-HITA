package com.stupidtree.hitax.eas

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.TrainingPlanService
import com.stupidtree.hitax.data.model.eas.EASToken
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlan
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlanDetail
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlanLevel
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.source.web.eas.EASource
import com.stupidtree.hitax.data.source.web.eas.ShenzhenTrainingPlanParser
import org.jsoup.Connection

/** 复用 EAS 登录 cookies，按 HITA_Android 的深圳教务接口拉取培养方案。 */
internal class TrainingPlanServiceImpl(private val application: Application) : TrainingPlanService {

    private val repo get() = EASRepository.getInstance(application)
    private val source = EASource()

    override fun plans(): LiveData<DataState<List<ShenzhenTrainingPlan>>> {
        val result = MutableLiveData<DataState<List<ShenzhenTrainingPlan>>>()
        Thread {
            val token = repo.getEasToken()
            if (token.cookies.isEmpty()) {
                result.postValue(DataState(DataState.STATE.NOT_LOGGED_IN, "请先连接深圳 Web 教务"))
                return@Thread
            }
            runCatching {
                val identityResp = source.jwPost(token, "/UserManager/queryxsxx", emptyMap())
                if (expired(identityResp)) error("深圳 Web 会话已失效")
                val identity = if (identityResp.statusCode() == 200) {
                    ShenzhenTrainingPlanParser.parseIdentity(identityResp.body())
                } else null
                val major = identity?.major.orEmpty().ifBlank { token.major.orEmpty() }
                val grade = identity?.grade.orEmpty().ifBlank { token.grade.orEmpty() }
                    .let { Regex("20\\d{2}").find(it)?.value.orEmpty() }
                val studentType = identity?.studentType.orEmpty().ifBlank { token.getStudentType() }
                if (major.isBlank()) error("教务系统未返回你的专业信息")
                val level = if (studentType == "1") {
                    ShenzhenTrainingPlanLevel.UNDERGRADUATE
                } else ShenzhenTrainingPlanLevel.POSTGRADUATE
                val baseForm = linkedMapOf(
                    "sf_request_type" to "ajax", "key" to "", "xkdm" to "", "yxdm" to "",
                    "zydm" to "", "zyfxdm" to "",
                    "bbh" to if (level == ShenzhenTrainingPlanLevel.POSTGRADUATE) "202603" else "",
                    "ywdm" to "", "falx" to if (level == ShenzhenTrainingPlanLevel.UNDERGRADUATE) "3" else "2",
                    "njdm" to if (level == ShenzhenTrainingPlanLevel.UNDERGRADUATE) grade else "",
                    "cxby" to "", "pylb" to if (level == ShenzhenTrainingPlanLevel.UNDERGRADUATE) "1" else "2",
                    "order1" to "", "order2" to "", "falxdm" to "", "kzsjqx" to "0",
                    "py_xssfcxzj_zx" to "1", "py_xssfcxzj_fx" to "1", "sfdl" to "",
                    "pageNum" to "1", "pageSize" to "500",
                )
                val plans = mutableListOf<ShenzhenTrainingPlan>()
                var page = 1
                var pages = 1
                do {
                    val response = source.jwPost(
                        token,
                        "/faxq/query?sf_request_type=ajax",
                        baseForm + ("pageNum" to page.toString()),
                        "/faxq/query",
                    )
                    if (expired(response)) error("深圳 Web 会话已失效")
                    if (response.statusCode() != 200) error("培养方案列表请求失败")
                    plans += ShenzhenTrainingPlanParser.parsePlans(response.body(), level).orEmpty()
                    if (page == 1) pages = ShenzhenTrainingPlanParser.parsePageCount(response.body())
                    page++
                } while (page <= pages)
                val matched = ShenzhenTrainingPlanParser.matchPersonalPlans(plans, major)
                if (matched.isEmpty()) error("未找到与“$major”匹配的个人培养方案")
                DataState(matched, DataState.STATE.SUCCESS)
            }.onSuccess { result.postValue(it) }
                .onFailure { result.postValue(DataState(DataState.STATE.FETCH_FAILED, it.message)) }
        }.start()
        return result
    }

    override fun detail(plan: ShenzhenTrainingPlan): LiveData<DataState<ShenzhenTrainingPlanDetail>> {
        val result = MutableLiveData<DataState<ShenzhenTrainingPlanDetail>>()
        Thread {
            val token = repo.getEasToken()
            if (token.cookies.isEmpty()) {
                result.postValue(DataState(DataState.STATE.NOT_LOGGED_IN, "请先连接深圳 Web 教务"))
                return@Thread
            }
            runCatching {
                val groups = if (plan.level == ShenzhenTrainingPlanLevel.POSTGRADUATE) {
                    val response = source.jwPost(
                        token,
                        "/Zdxpyfakz/queryKzTree?sf_request_type=ajax",
                        mapOf("fah" to plan.id, "bgid" to plan.changeId, "pylb" to "2", "sfcx" to ""),
                        "/Zdxpyfakz/query",
                    )
                    if (expired(response)) error("深圳 Web 会话已失效")
                    if (response.statusCode() != 200) error("培养方案课组请求失败")
                    ShenzhenTrainingPlanParser.parseGroups(response.body()).orEmpty()
                } else emptyList()

                val path: String
                val referer: String
                val baseForm: Map<String, String>
                if (plan.level == ShenzhenTrainingPlanLevel.UNDERGRADUATE) {
                    path = "/Njpyfakc/queryList?sf_request_type=ajax"
                    referer = "/Njpyfakc/query"
                    baseForm = linkedMapOf(
                        "bglx" to "", "multiple" to "false", "sfcx" to "", "pylx" to "1",
                        "pylb" to "1", "fah" to plan.id, "bgid" to plan.changeId,
                        "kcmc" to "", "yxdm" to "", "xqdm" to "", "kclbdm" to "",
                        "kcxzdm" to "", "order1" to "", "order2" to "", "pageNum" to "1",
                        "pageSize" to "500",
                    )
                } else {
                    path = "/Zdxpyfakz/queryFaKzkc?sf_request_type=ajax"
                    referer = "/Zdxpyfakz/query"
                    baseForm = linkedMapOf(
                        "sfcx" to "", "pylx" to "2", "pylb" to "2", "fah" to plan.id,
                        "bgid" to plan.changeId, "kzid" to "", "kcmc" to "",
                        "zyfx" to plan.majorCode, "yxdm" to "", "xqdm" to "",
                        "order1" to "", "order2" to "", "pageNum" to "1", "pageSize" to "500",
                    )
                }
                val courses = mutableListOf<com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlanCourse>()
                var page = 1
                var pages = 1
                do {
                    val response = source.jwPost(
                        token, path, baseForm + ("pageNum" to page.toString()), referer,
                    )
                    if (expired(response)) error("深圳 Web 会话已失效")
                    if (response.statusCode() != 200) error("培养方案课程请求失败")
                    courses += ShenzhenTrainingPlanParser.parseCourses(response.body()).orEmpty()
                    if (page == 1) pages = ShenzhenTrainingPlanParser.parsePageCount(response.body())
                    page++
                } while (page <= pages)
                if (courses.isEmpty()) error("该培养方案暂未返回课程")
                DataState(
                    ShenzhenTrainingPlanParser.combine(
                        plan, groups,
                        courses.distinctBy {
                            listOf(it.groupId, it.courseCode, it.courseName, it.recommendedTerm)
                        },
                    ),
                    DataState.STATE.SUCCESS,
                )
            }.onSuccess { result.postValue(it) }
                .onFailure { result.postValue(DataState(DataState.STATE.FETCH_FAILED, it.message)) }
        }.start()
        return result
    }

    private fun expired(response: Connection.Response): Boolean =
        response.statusCode() == 401 || response.statusCode() == 302
}
