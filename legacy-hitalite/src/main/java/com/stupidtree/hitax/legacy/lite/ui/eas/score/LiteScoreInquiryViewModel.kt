package com.stupidtree.hitax.legacy.lite.ui.eas.score

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.component.data.DataState
import com.stupidtree.component.data.MTransformations
import com.stupidtree.component.data.Trigger
import com.stupidtree.hita.api.ScoreReport
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * HITA_Lite 成绩查询数据门面。
 *
 * 前置条件：LiteHostBridge 已注入宿主 ScoreService；缺失或未登录时返回 NOT_LOGGED_IN。
 * 后置条件：学期列表来自 [com.stupidtree.hita.api.ScoreService.terms]，
 * 成绩来自 [com.stupidtree.hita.api.ScoreService.report]；不创建第二份成绩存储。
 * 不变式：只有选中学期后才发起成绩请求。
 * 违约处理：服务缺失时所有请求返回 NOT_LOGGED_IN，界面显示空态。
 */
class LiteScoreInquiryViewModel : ViewModel() {

    private val service = LiteHostBridge.scores

    private val pageController = MutableLiveData<Trigger>()

    val termsLiveData: LiveData<DataState<List<TermItem>>> =
        MTransformations.switchMap(pageController) { trigger ->
            if (trigger.isActioning) {
                service?.terms()
                    ?: MutableLiveData<DataState<List<TermItem>>>(
                        DataState(DataState.STATE.NOT_LOGGED_IN)
                    )
            } else {
                MutableLiveData<DataState<List<TermItem>>>(
                    DataState(DataState.STATE.NOTHING)
                )
            }
        }

    val selectedTermLiveData = MutableLiveData<TermItem?>(null)

    /** true 表示包含补考成绩（对应 ScoreService.report 的 includeResit）。 */
    val includeResitLiveData = MutableLiveData(false)

    val reportLiveData: LiveData<DataState<ScoreReport>> =
        MTransformations.switchMap(selectedTermLiveData, includeResitLiveData) { pair ->
            val term = pair.first
            if (term == null) {
                MutableLiveData<DataState<ScoreReport>>(DataState(DataState.STATE.NOTHING))
            } else {
                service?.report(term, pair.second)
                    ?: MutableLiveData<DataState<ScoreReport>>(
                        DataState(DataState.STATE.NOT_LOGGED_IN)
                    )
            }
        }

    fun startRefresh() {
        pageController.value = Trigger.actioning
    }
}
