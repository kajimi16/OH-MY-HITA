package com.stupidtree.hitax.legacy.lite.ui.event.add

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteEventDialogPickCourseTimeBinding
import com.stupidtree.hitax.legacy.lite.databinding.LiteEventDynamicAddEventDateItemBinding
import com.stupidtree.hitax.utils.TimeTools
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder
import com.stupidtree.style.widgets.TransparentDialog
import java.util.Calendar

/**
 * 选择上课时间段的弹窗。
 *
 * 前置条件：setInitialValue 传入宿主 Timetable 及可选的初始 LiteCourseTime。
 * 后置条件：确定后通过 OnTimeSelectedListener 返回周几、节次与周数。
 * 违约处理：未选择周数时提示并保持弹窗打开。
 */
class LitePickCourseTimeDialog :
    TransparentDialog<LiteEventDialogPickCourseTimeBinding>() {

    interface OnTimeSelectedListener {
        fun onSelected(data: LiteCourseTime)
    }

    var onTimeSelectedListener: OnTimeSelectedListener? = null
    private var initTimetable: Timetable? = null
    private var initCourseTime: LiteCourseTime? = null
    private var listAdapter: PickWeekListAdapter? = null
    private var datesAdapter: SelectedDateAdapter? = null

    private val rangeLiveData = MutableLiveData<Pair<Int, TimePeriodInDay>>()
    private val weeksLiveData = MutableLiveData<List<Boolean>>()
    private val selectedTimesLiveData = MutableLiveData<List<Long>>()

    override fun getLayoutId(): Int = R.layout.lite_event_dialog_pick_course_time

    override fun initViewBinding(view: View): LiteEventDialogPickCourseTimeBinding =
        LiteEventDialogPickCourseTimeBinding.bind(view)

    fun setInitialValue(timetable: Timetable?, courseTime: LiteCourseTime?): LitePickCourseTimeDialog =
        apply {
            initTimetable = timetable
            initCourseTime = courseTime
        }

    fun setSelectListener(listener: OnTimeSelectedListener): LitePickCourseTimeDialog = apply {
        onTimeSelectedListener = listener
    }
    private fun bindLiveData() {
        weeksLiveData.observe(this) { weeks ->
            listAdapter?.notifyData(weeks)
        }
        rangeLiveData.observe(this) { range ->
            val timetable = initTimetable ?: return@observe
            binding.pickdow.currentIndex = (range.first - 1).coerceIn(0, 6)
            val period = timetable.transformCourseNumber(range.second)
            binding.pickfromt.currentIndex = (period.first - 1).coerceAtLeast(0)
            binding.picktot.currentIndex = (period.second - 1).coerceAtLeast(0)
        }
        selectedTimesLiveData.observe(this) { times ->
            val dates = times.map { time ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = time
                TimeTools.getDateString(requireContext(), calendar, true, TimeTools.TTY_NONE)
            }
            datesAdapter?.notifyItemChangedSmooth(dates, notifyNormalItem = false)
        }
    }
    override fun initViews(view: View) {
        val dows = resources.getStringArray(R.array.lite_event_dow2).toMutableList()
        binding.pickdow.setEntries(dows)
        listAdapter = PickWeekListAdapter(
            requireContext(),
            getColorPrimary(),
            getTextColorSecondary(),
        )
        binding.weekList.adapter = listAdapter
        binding.weekList.layoutManager = GridLayoutManager(context, 5)
        listAdapter?.onItemCheckListener = object : PickWeekListAdapter.OnItemCheckListener {
            override fun onItemChecked(position: Int, checked: Boolean) {
                selectedTimesLiveData.value = getSelectedDates()
            }
        }
        datesAdapter = SelectedDateAdapter(requireContext(), mutableListOf())
        binding.dateList.adapter = datesAdapter
        binding.dateList.layoutManager = GridLayoutManager(context, 4)
        binding.cancel.setOnClickListener { dismiss() }
        binding.done.setOnClickListener { confirmSelection() }
        binding.pickfromt.setOnWheelChangedListener { _, _, newIndex ->
            if (binding.picktot.currentIndex < newIndex) {
                binding.picktot.currentIndex = newIndex
            }
            initTimetable?.scheduleStructure?.getOrNull(newIndex)?.let {
                binding.fromTime.text = it.from.toString()
            }
        }
        binding.picktot.setOnWheelChangedListener { _, _, newIndex ->
            if (binding.pickfromt.currentIndex > newIndex) {
                binding.pickfromt.currentIndex = newIndex
            }
            initTimetable?.scheduleStructure?.getOrNull(newIndex)?.let {
                binding.toTime.text = it.to.toString()
            }
        }
        binding.pickdow.setOnWheelChangedListener { _, _, _ ->
            selectedTimesLiveData.value = getSelectedDates()
        }
        bindLiveData()
    }
    private fun confirmSelection() {
        val timetable = initTimetable ?: return
        val result = LiteCourseTime().apply {
            dow = binding.pickdow.currentIndex + 1
            period = timetable.transformTimePeriod(
                binding.pickfromt.currentIndex + 1,
                binding.picktot.currentIndex + 1,
            )
            weeks = listAdapter?.getSelectedWeeks().orEmpty()
        }
        if (result.weeks.isEmpty()) {
            Toast.makeText(context, R.string.lite_event_ade_pick_weeks, Toast.LENGTH_SHORT).show()
        } else {
            onTimeSelectedListener?.onSelected(result)
            dismiss()
        }
    }

    private fun getSelectedDates(): List<Long> {
        val timetable = initTimetable ?: return emptyList()
        val selected = listAdapter?.getSelectedWeeks().orEmpty()
        return selected.map { week ->
            timetable.getTimestamps(
                week,
                binding.pickdow.currentIndex + 1,
                binding.pickfromt.currentIndex + 1,
                binding.picktot.currentIndex + 1,
            )[0]
        }
    }
    override fun onStart() {
        super.onStart()
        val timetable = initTimetable ?: return
        val weeks = MutableList(timetable.getWeekNumber(timetable.endTime.time).coerceAtLeast(0)) {
            false
        }
        val entries = timetable.scheduleStructure.indices.map { index ->
            getString(R.string.lite_event_period, index + 1)
        }
        binding.pickfromt.setEntries(entries)
        binding.picktot.setEntries(entries)
        timetable.scheduleStructure.firstOrNull()?.let {
            binding.fromTime.text = it.from.toString()
            binding.toTime.text = it.to.toString()
        }
        initCourseTime?.let { courseTime ->
            rangeLiveData.value = Pair(courseTime.dow, courseTime.period)
            courseTime.weeks.forEach { week ->
                while (weeks.size < week) weeks.add(false)
                if (week >= 1) weeks[week - 1] = true
            }
        }
        weeksLiveData.value = weeks
    }
    internal class PickWeekListAdapter(
        private val context: Context,
        private val primaryColor: Int,
        private val secondaryColor: Int,
    ) : RecyclerView.Adapter<PickWeekListAdapter.WeekHolder>() {

        interface OnItemCheckListener {
            fun onItemChecked(position: Int, checked: Boolean)
        }

        var onItemCheckListener: OnItemCheckListener? = null
        private val weeks = mutableListOf<Boolean>()

        fun notifyData(data: List<Boolean>) {
            weeks.clear()
            weeks.addAll(data)
            notifyDataSetChanged()
        }

        fun getSelectedWeeks(): List<Int> =
            weeks.indices.filter { weeks[it] }.map { it + 1 }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.lite_event_dynamic_pick_week_item, parent, false)
            return WeekHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: WeekHolder, position: Int) {
            if (position == weeks.size) {
                holder.card.setCardBackgroundColor(secondaryColor)
                holder.text.setTextColor(secondaryColor)
                holder.text.text = "＋"
                holder.card.setOnClickListener {
                    repeat(5) {
                        weeks.add(false)
                        notifyItemInserted(weeks.size)
                    }
                }
                return
            }
            val selected = weeks[position]
            holder.card.setCardBackgroundColor(if (selected) primaryColor else secondaryColor)
            holder.text.setTextColor(secondaryColor)
            holder.text.text = (position + 1).toString()
            holder.card.setOnClickListener {
                weeks[position] = !weeks[position]
                onItemCheckListener?.onItemChecked(position, weeks[position])
                notifyItemChanged(position)
            }
        }

        override fun getItemCount(): Int = weeks.size + 1

        class WeekHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.card)
            val text: TextView = view.findViewById(R.id.text)
        }
    }
    class SelectedDateAdapter(
        context: Context,
        beans: MutableList<String>,
    ) : BaseListAdapter<String, SelectedDateAdapter.DateHolder>(context, beans) {

        class DateHolder(binding: LiteEventDynamicAddEventDateItemBinding) :
            BaseViewHolder<LiteEventDynamicAddEventDateItemBinding>(binding)

        override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
            LiteEventDynamicAddEventDateItemBinding.inflate(mInflater, parent, false)

        override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): DateHolder =
            DateHolder(viewBinding as LiteEventDynamicAddEventDateItemBinding)

        override fun bindHolder(holder: DateHolder, data: String?, position: Int) {
            holder.binding.date.text = data
        }
    }
}
