package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.ExamItem

/** 考试列表（API 7）。由 plugin-eas 提供。 */
interface ExamService {
    fun exams(): LiveData<DataState<List<ExamItem>>>

    companion object {
        val KEY = ServiceKey<ExamService>("hita.eas.exams")
    }
}
