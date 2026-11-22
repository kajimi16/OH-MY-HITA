package com.stupidtree.hitax.legacy.lite.ui.event.add

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.component.data.MTransformations
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteEventDialogAddBinding
import com.stupidtree.style.widgets.DialogAutoEditText
import com.stupidtree.style.widgets.DialogSelectableLiveList
import com.stupidtree.style.widgets.TransparentModeledBottomSheetDialog

/**
 * 添加课时 / 科目底部弹窗。
 *
 * 前置条件：宿主已注入 TimetableService；可选的初始课表通过 setter 传入。
 * 后置条件：完成按钮在课表、科目、时间与名称齐备后显示；写入交给宿主服务。
 * 违约处理：服务缺失时下拉列表为空，弹窗不写入。
 */
class LiteAddEventDialog :
    TransparentModeledBottomSheetDialog<LiteAddEventViewModel, LiteEventDialogAddBinding>() {

    private var addSubjectMode = false
    private var initTimetable: Timetable? = null
    private var initSubject: TermSubject? = null
    private var initCourseTime: LiteCourseTime? = null

    override fun getViewModelClass(): Class<LiteAddEventViewModel> =
        LiteAddEventViewModel::class.java

    override fun getLayoutId(): Int = R.layout.lite_event_dialog_add

    override fun initViewBinding(view: View): LiteEventDialogAddBinding =
        LiteEventDialogAddBinding.bind(view)

    fun setInitTimetable(timetable: Timetable?): LiteAddEventDialog = apply {
        initTimetable = timetable
    }

    fun setInitTime(dow: Int, week: Int, period: TimePeriodInDay): LiteAddEventDialog = apply {
        initCourseTime = LiteCourseTime().apply {
            this.dow = dow
            weeks = mutableListOf(week)
            this.period = period
        }
    }

    fun setInitSubject(subject: TermSubject): LiteAddEventDialog = apply {
        initSubject = subject
    }
    override fun initViews(view: View) {
        binding?.title?.setText(
            if (addSubjectMode) R.string.lite_event_add_subject else R.string.lite_event_ade_title
        )
        binding?.cancel?.setOnClickListener { dismiss() }
        viewModel.doneLiveData.observe(this) { done ->
            if (done) binding?.adeBtDone?.show() else binding?.adeBtDone?.hide()
        }
        viewModel.timetableLiveData.observe(this) { state ->
            binding?.pickTimetable?.visibility =
                if (state.state == DataState.STATE.FETCH_FAILED) View.GONE else View.VISIBLE
            if (state.state == DataState.STATE.SUCCESS) {
                binding?.pickTimetableIcon?.setColorFilter(getColorPrimary())
                binding?.pickTimetableText?.setTextColor(getColorPrimary())
                binding?.pickTimetable?.setCardBackgroundColor(getColorPrimary())
                binding?.pickTimetableText?.text = state.data?.name
            } else {
                binding?.pickTimetableText?.setText(R.string.lite_event_ade_pick_timetable)
                binding?.pickTimetable?.setCardBackgroundColor(getTextColorSecondary())
                binding?.pickTimetableText?.setTextColor(getTextColorSecondary())
                binding?.pickTimetableIcon?.clearColorFilter()
            }
        }
        viewModel.subjectLiveData.observe(this) { state ->
            binding?.pickSubject?.visibility =
                if (state.state == DataState.STATE.FETCH_FAILED ||
                    state.state == DataState.STATE.SPECIAL
                ) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            if (state.state == DataState.STATE.SUCCESS) {
                binding?.pickSubjectIcon?.setColorFilter(getColorPrimary())
                binding?.pickSubjectText?.setTextColor(getColorPrimary())
                binding?.pickSubject?.setCardBackgroundColor(getColorPrimary())
                binding?.pickSubjectText?.text = state.data?.name
                binding?.name?.setText(state.data?.name)
            } else {
                binding?.pickSubjectText?.setText(R.string.lite_event_ade_pick_subject)
                binding?.pickSubject?.setCardBackgroundColor(getTextColorSecondary())
                binding?.pickSubjectText?.setTextColor(getTextColorSecondary())
                binding?.pickSubjectIcon?.clearColorFilter()
            }
        }
        viewModel.teacherLiveData.observe(this) { state ->
            binding?.pickTeacher?.visibility =
                if (state.state == DataState.STATE.FETCH_FAILED) View.GONE else View.VISIBLE
            if (state.state == DataState.STATE.SUCCESS) {
                binding?.pickTeacher?.setCardBackgroundColor(getColorPrimary())
                binding?.pickTeacherIcon?.setColorFilter(getColorPrimary())
                binding?.pickTeacherText?.setTextColor(getColorPrimary())
                binding?.pickTeacherText?.text = state.data
                binding?.pickTeacherCancel?.visibility = View.VISIBLE
            } else {
                binding?.pickTeacherText?.setText(R.string.lite_event_ade_pick_teacher)
                binding?.pickTeacher?.setCardBackgroundColor(getColorPrimary())
                binding?.pickTeacherText?.setTextColor(getTextColorSecondary())
                binding?.pickTeacherIcon?.clearColorFilter()
                binding?.pickTeacherCancel?.visibility = View.GONE
            }
        }
        viewModel.locationLiveData.observe(this) { state ->
            binding?.pickLocation?.visibility =
                if (state.state == DataState.STATE.FETCH_FAILED) View.GONE else View.VISIBLE
            if (state.state == DataState.STATE.SUCCESS) {
                binding?.pickLocation?.setCardBackgroundColor(getColorPrimary())
                binding?.pickLocationIcon?.setColorFilter(getColorPrimary())
                binding?.pickLocationText?.setTextColor(getColorPrimary())
                binding?.pickLocationText?.text = state.data
                binding?.pickLocationCancel?.visibility = View.VISIBLE
            } else {
                binding?.pickLocationText?.setText(R.string.lite_event_ade_pick_location)
                binding?.pickLocation?.setCardBackgroundColor(getTextColorSecondary())
                binding?.pickLocationText?.setTextColor(getTextColorSecondary())
                binding?.pickLocationIcon?.clearColorFilter()
                binding?.pickLocationCancel?.visibility = View.GONE
            }
        }
        viewModel.timeRangeLiveDate.observe(this) { state ->
            binding?.pickTime?.visibility =
                if (state.state == DataState.STATE.FETCH_FAILED) View.GONE else View.VISIBLE
            val courseTime = state.data
            if (state.state == DataState.STATE.SUCCESS && courseTime != null) {
                binding?.pickTime?.setCardBackgroundColor(getColorPrimary())
                binding?.pickTimeIcon?.setColorFilter(getColorPrimary())
                binding?.timeShow?.setTextColor(getColorPrimary())
                val dow = resources.getStringArray(R.array.lite_event_dow1)[courseTime.dow - 1]
                val timeText = "$dow ${courseTime.period.from}-${courseTime.period.to}"
                binding?.timeShow?.text = getString(
                    R.string.lite_event_week_with_day,
                    formatWeeks(courseTime.weeks),
                    timeText,
                )
            } else {
                binding?.timeShow?.setText(R.string.lite_event_ade_set_time_period)
                binding?.pickTime?.setCardBackgroundColor(getTextColorSecondary())
                binding?.timeShow?.setTextColor(getTextColorSecondary())
                binding?.pickTimeIcon?.clearColorFilter()
            }
        }
        binding?.pickTimetable?.setOnClickListener {
            DialogSelectableLiveList<Timetable>()
                .setTitle(R.string.lite_event_ade_pick_timetable)
                .setInitValue(viewModel.timetableLiveData.value?.data)
                .setDataLoader(object : DialogSelectableLiveList.DataLoader<Timetable> {
                    override fun loadData():
                        LiveData<List<DialogSelectableLiveList.ItemData<Timetable>>> {
                        val source = LiteHostBridge.timetable?.getTimetables()
                            ?: MutableLiveData<List<Timetable>>(emptyList())
                        return MTransformations.switchMap(source) { timetables ->
                            MutableLiveData(
                                timetables.map {
                                    DialogSelectableLiveList.ItemData(it.name, it)
                                }
                            )
                        }
                    }
                })
                .setOnConfirmListener(object :
                    DialogSelectableLiveList.OnConfirmListener<Timetable> {
                    override fun onConfirm(title: String?, key: Timetable) {
                        viewModel.timetableLiveData.value = DataState(key)
                    }
                })
                .show(childFragmentManager, "lite_event_pick_timetable")
        }
        binding?.pickSubject?.setOnClickListener {
            val timetableId = viewModel.timetableLiveData.value?.data?.id ?: ""
            DialogSelectableLiveList<TermSubject>()
                .setTitle(R.string.lite_event_ade_pick_subject)
                .setInitValue(viewModel.subjectLiveData.value?.data)
                .setDataLoader(object : DialogSelectableLiveList.DataLoader<TermSubject> {
                    override fun loadData():
                        LiveData<List<DialogSelectableLiveList.ItemData<TermSubject>>> {
                        val source = LiteHostBridge.timetable?.getSubjects(timetableId)
                            ?: MutableLiveData<List<TermSubject>>(emptyList())
                        return MTransformations.switchMap(source) { subjects ->
                            MutableLiveData(
                                subjects.map {
                                    DialogSelectableLiveList.ItemData(it.name, it)
                                }
                            )
                        }
                    }
                })
                .setOnConfirmListener(object :
                    DialogSelectableLiveList.OnConfirmListener<TermSubject> {
                    override fun onConfirm(title: String?, key: TermSubject) {
                        viewModel.subjectLiveData.value = DataState(key)
                    }
                })
                .show(childFragmentManager, "lite_event_pick_subject")
        }
        binding?.pickTime?.setOnClickListener {
            val timetable = viewModel.timetableLiveData.value?.data
            if (timetable != null) {
                LitePickCourseTimeDialog()
                    .setInitialValue(timetable, viewModel.timeRangeLiveDate.value?.data)
                    .setSelectListener(object : LitePickCourseTimeDialog.OnTimeSelectedListener {
                        override fun onSelected(data: LiteCourseTime) {
                            if (data.weeks.isEmpty()) {
                                viewModel.timetableLiveData.value =
                                    DataState(DataState.STATE.NOTHING)
                            } else {
                                viewModel.timeRangeLiveDate.value = DataState(data)
                            }
                        }
                    })
                    .show(childFragmentManager, "lite_event_pick_course_time")
            }
        }
        binding?.pickTeacherCancel?.setOnClickListener {
            viewModel.teacherLiveData.value = DataState(DataState.STATE.NOTHING)
        }
        binding?.pickTeacher?.setOnClickListener {
            DialogAutoEditText()
                .setTitle(getString(R.string.lite_event_ade_pick_teacher))
                .setOnConfirmListener(object : DialogAutoEditText.OnConfirmListener {
                    override fun OnConfirm(content: String) {
                        viewModel.teacherLiveData.value = DataState(content)
                    }
                })
                .setInitValue(viewModel.teacherLiveData.value?.data ?: "")
                .setDataLoader(object : DialogAutoEditText.DataLoader {
                    override fun loadData(text: String): LiveData<List<String>> =
                        LiteHostBridge.resourceUi?.searchTeacherNames(text)
                            ?: MutableLiveData(emptyList())
                })
                .show(childFragmentManager, "lite_event_pick_teacher")
        }
        binding?.pickLocationCancel?.setOnClickListener {
            viewModel.locationLiveData.value = DataState(DataState.STATE.NOTHING)
        }
        binding?.pickLocation?.setOnClickListener {
            DialogAutoEditText()
                .setTitle(getString(R.string.lite_event_ade_pick_location))
                .setOnConfirmListener(object : DialogAutoEditText.OnConfirmListener {
                    override fun OnConfirm(content: String) {
                        viewModel.locationLiveData.value = DataState(content)
                    }
                })
                .setInitValue(viewModel.locationLiveData.value?.data ?: "")
                .setDataLoader(object : DialogAutoEditText.DataLoader {
                    override fun loadData(text: String): LiveData<List<String>> =
                        LiteHostBridge.timetable?.searchLocation(text)
                            ?: MutableLiveData(emptyList())
                })
                .show(childFragmentManager, "lite_event_pick_location")
        }
        binding?.adeBtDone?.setOnClickListener {
            viewModel.createEvent()
            dismiss()
        }
        binding?.name?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                Unit

            override fun afterTextChanged(s: Editable?) {
                viewModel.nameLiveData.value = s?.toString()
            }
        })
        viewModel.init(addSubjectMode, initTimetable, initSubject, initCourseTime)
    }
    private fun formatWeeks(weeks: List<Int>): String {
        val selected = weeks.toHashSet()
        val fragments = mutableListOf<String>()
        for (week in selected.sorted()) {
            if (selected.contains(week - 1)) continue
            var last = week
            while (selected.contains(last + 1)) last++
            when (last) {
                week -> fragments.add("$week")
                week + 1 -> {
                    fragments.add("$week")
                    fragments.add("$last")
                }
                else -> fragments.add("$week-$last")
            }
        }
        return fragments.joinToString(", ")
    }

    companion object {
        fun newInstance(addSubjectMode: Boolean = false): LiteAddEventDialog =
            LiteAddEventDialog().apply { this.addSubjectMode = addSubjectMode }
    }
}
