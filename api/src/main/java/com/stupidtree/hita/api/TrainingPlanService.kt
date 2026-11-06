package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlan
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlanDetail

/** 深圳教务培养方案（API 7）。由 plugin-eas 提供。 */
interface TrainingPlanService {
    fun plans(): LiveData<DataState<List<ShenzhenTrainingPlan>>>

    fun detail(plan: ShenzhenTrainingPlan): LiveData<DataState<ShenzhenTrainingPlanDetail>>

    companion object {
        val KEY = ServiceKey<TrainingPlanService>("hita.eas.trainingPlan")
    }
}
