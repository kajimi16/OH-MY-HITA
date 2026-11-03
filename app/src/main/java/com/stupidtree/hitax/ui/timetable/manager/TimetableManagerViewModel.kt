package com.stupidtree.hitax.ui.timetable.manager

import com.stupidtree.hitax.HitaServices
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.model.timetable.IcsImportResult
import com.stupidtree.component.data.DataState
import java.io.InputStream

class TimetableManagerViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 仓库区
     */
    private val timetableRepository = HitaServices.timetable(application)
    val timetablesLiveData:LiveData<List<Timetable>> = timetableRepository.getTimetables()

    private val exportController = MutableLiveData<Timetable>()
    val exportToICSResult = exportController.switchMap {
        return@switchMap timetableRepository.exportToICS(it.name ?: "课表", it.id)
    }


    fun startDeleteTimetables(timetables:List<Timetable>){
        timetableRepository.actionDeleteTimetables(timetables)
    }

    fun startNewTimetable(){
        timetableRepository.actionNewTimetable()
    }

    fun exportToIcs(timetable: Timetable) {
        exportController.value = timetable
    }
    
    /**
     * 从 ICS 文件导入课表
     */
    fun importFromICSAsNewTimetable(
        inputStream: InputStream,
        sourceName: String?
    ): LiveData<DataState<IcsImportResult>> {
        return timetableRepository.importFromICSAsNewTimetable(inputStream, sourceName)
    }
}
