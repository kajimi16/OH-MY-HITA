package com.stupidtree.hitax.ui.event

import com.stupidtree.hitax.HitaServices
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject

class EventItemViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 仓库区
     */
    private val subjectRepository = HitaServices.timetable(application)
    private val timetableRepository =HitaServices.timetable(application)

    /**
     * 数据区
     */
    val eventItemLiveData: MutableLiveData<EventItem> = MutableLiveData()

    val progressLiveData: LiveData<Pair<Int, Int>> = eventItemLiveData.switchMap {
        return@switchMap subjectRepository.getProgressOfSubject(it.subjectId, it.from.time)
    }

    val subjectLiveData: LiveData<TermSubject> = eventItemLiveData.switchMap {
        subjectRepository.getSubjectById(it.subjectId)
    }

    fun changeSubjectColor(color: Int) {
        subjectLiveData.value?.let { subject ->
            subjectRepository.actionChangeSubjectColor(subject.id, color)
        }
    }

    fun delete(){
        eventItemLiveData.value?.let {
            timetableRepository.actionDeleteEvents(listOf(it))
        }

    }
}
