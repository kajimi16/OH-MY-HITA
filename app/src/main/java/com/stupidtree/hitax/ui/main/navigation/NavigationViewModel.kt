package com.stupidtree.hitax.ui.main.navigation

import com.stupidtree.hitax.HApplication
import com.stupidtree.hita.api.TimetableService
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.component.data.Trigger
import com.stupidtree.hitax.data.model.timetable.Timetable

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    /** 插件缺席（空壳构建）时为 null，界面显示空状态而不是崩溃。 */
    private val timetableService: TimetableService? =
        (application as HApplication).pluginHost.service(TimetableService.KEY)

    private val recentTimetableController = MutableLiveData<Trigger>()
    val recentTimetableLiveData: LiveData<Timetable?> = recentTimetableController.switchMap {
        timetableService?.getRecentTimetable() ?: MutableLiveData(null)
    }
    val timetableCountLiveData: LiveData<Int> = recentTimetableController.switchMap {
        timetableService?.getTimetableCount() ?: MutableLiveData(0)
    }


    fun startRefresh() {
        recentTimetableController.value = Trigger.actioning
    }
}