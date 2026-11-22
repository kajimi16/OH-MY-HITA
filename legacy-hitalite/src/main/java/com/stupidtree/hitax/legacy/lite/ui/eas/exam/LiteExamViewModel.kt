package com.stupidtree.hitax.legacy.lite.ui.eas.exam

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.component.data.DataState
import com.stupidtree.component.data.MTransformations
import com.stupidtree.component.data.Trigger
import com.stupidtree.hitax.data.model.eas.ExamItem
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * HITA_Lite 考试查询数据门面。
 *
 * 前置条件：LiteHostBridge 已注入宿主 ExamService。
 * 后置条件：列表来自 [com.stupidtree.hita.api.ExamService.exams]；不创建第二份考试存储。
 * 违约处理：服务缺失时返回 NOT_LOGGED_IN，界面显示空态。
 */
class LiteExamViewModel : ViewModel() {

    private val service = LiteHostBridge.exams

    private val pageController = MutableLiveData<Trigger>()

    val examInfoLiveData: LiveData<DataState<List<ExamItem>>> =
        MTransformations.switchMap(pageController) { trigger ->
            if (trigger.isActioning) {
                service?.exams()
                    ?: MutableLiveData<DataState<List<ExamItem>>>(
                        DataState(DataState.STATE.NOT_LOGGED_IN)
                    )
            } else {
                MutableLiveData<DataState<List<ExamItem>>>(
                    DataState(DataState.STATE.NOTHING)
                )
            }
        }

    fun startRefresh() {
        pageController.value = Trigger.actioning
    }
}
