package com.stupidtree.hitax.ui.main.navigation

import com.stupidtree.hitax.HitaServices
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.component.data.Trigger
import com.stupidtree.hitax.data.model.timetable.Timetable

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    private val timetableRepository = HitaServices.timetable(application)

    private val recentTimetableController = MutableLiveData<Trigger>()
    val recentTimetableLiveData: LiveData<Timetable?> = recentTimetableController.switchMap {
            return@switchMap timetableRepository.getRecentTimetable()
        }
    val timetableCountLiveData: LiveData<Int> = recentTimetableController.switchMap {
            return@switchMap timetableRepository.getTimetableCount()
        }


    fun startRefresh() {
        recentTimetableController.value = Trigger.actioning
    }
}