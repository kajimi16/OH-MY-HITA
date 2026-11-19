package com.stupidtree.hitax.ui.eas.exam

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.component.data.DataState
import com.stupidtree.component.data.Trigger
import com.stupidtree.hitax.data.model.eas.ExamItem
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.source.local.ExamMemoStore
import com.stupidtree.hitax.ui.eas.EASViewModel

class ExamViewModel(application: Application) : EASViewModel(application) {
    private val easRepository = EASRepository.getInstance(application)
    private val memoStore = ExamMemoStore(application)

    private val pageController = MutableLiveData<Trigger>()

    val termsLiveData: LiveData<DataState<List<TermItem>>> = pageController.switchMap {
        easRepository.getAllTerms()
    }

    val selectedTermLiveData: MutableLiveData<TermItem> = MutableLiveData()
    val selectedExamTypeLiveData: MutableLiveData<ExamType> = MutableLiveData(ExamType.ALL)

    /** 最近一次教务返回的考试；删备忘录时不能把它一起丢掉。 */
    private var serverExams: List<ExamItem> = emptyList()

    private val mergedExams = MediatorLiveData<List<ExamItem>>()

    /** 合并源或任一筛选条件变化都会重算，不依赖界面手动调用刷新。 */
    val examInfoLiveData: LiveData<DataState<List<ExamItem>>> =
        MediatorLiveData<DataState<List<ExamItem>>>().apply {
            addSource(mergedExams) { publish(this) }
            addSource(selectedTermLiveData) { publish(this) }
            addSource(selectedExamTypeLiveData) { publish(this) }
        }

    init {
        mergedExams.addSource(easRepository.getExamInfo()) { state ->
            if (state.state == DataState.STATE.SUCCESS) {
                serverExams = state.data.orEmpty()
                mergedExams.value = serverExams + memoStore.load()
            }
        }
    }

    private fun publish(target: MutableLiveData<DataState<List<ExamItem>>>) {
        val data = mergedExams.value ?: return
        val term = selectedTermLiveData.value
        val type = selectedExamTypeLiveData.value
        target.value = DataState(
            data.filter { matchTerm(it, term) && matchType(it.examType, type) },
            DataState.STATE.SUCCESS,
        )
    }

    fun startRefresh() {
        pageController.value = Trigger.actioning
        mergedExams.value = serverExams + memoStore.load()
    }

    fun addMemo(item: ExamItem) {
        memoStore.add(item)
        mergedExams.value = serverExams + memoStore.load()
    }

    fun deleteMemo(memoId: String) {
        memoStore.delete(memoId)
        mergedExams.value = serverExams + memoStore.load()
    }

    private fun matchTerm(item: ExamItem, term: TermItem?): Boolean {
        if (term == null) return true
        val raw = (item.termName ?: "").trim()
        if (raw.isBlank()) {
            val examDate = (item.examDate ?: "").trim()
            val year = Regex("(\\d{4})").find(examDate)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (year != null) {
                val years = Regex("(\\d{4})").findAll(term.yearCode).mapNotNull { it.value.toIntOrNull() }.toList()
                if (years.isNotEmpty()) {
                    val min = years.minOrNull() ?: year
                    val max = years.maxOrNull() ?: year
                    return year in min..max
                }
            }
            return true
        }
        val compact = raw.replace("\\s".toRegex(), "")
        val targetName = term.name.replace("\\s".toRegex(), "")
        if (compact == targetName) return true
        val code = term.getCode().replace("\\s".toRegex(), "")
        if (compact == code) return true
        val yearName = term.yearName.replace("\\s".toRegex(), "")
        val termName = term.termName.replace("\\s".toRegex(), "")
        return (yearName.isNotBlank() && compact.contains(yearName) && compact.contains(termName))
    }

    private fun matchType(examType: String?, type: ExamType?): Boolean {
        if (type == null || type == ExamType.ALL) return true
        val raw = (examType ?: "").trim()
        if (raw.isBlank()) return true
        return when (type) {
            ExamType.MIDTERM -> raw.contains("期中")
            ExamType.FINAL -> raw.contains("期末")
            ExamType.ALL -> true
        }
    }

    enum class ExamType {
        ALL,
        MIDTERM,
        FINAL
    }
}
