package com.stupidtree.hitax.legacy.lite.ui.eas.classroom

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.stupidtree.hita.api.ClassroomRoom
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteRoomActivityClassroomDetailBinding
import com.stupidtree.hitax.utils.TimeTools
import com.stupidtree.style.widgets.TransparentBottomSheetDialog

/**
 * 单间教室的按天占用详情。
 *
 * 数据来自宿主 ClassroomRoom 的占用时段；节次时间来自当前课表结构。
 * 所有参数放在 arguments 里，重建 Fragment 时不依赖构造参数。
 */
class LiteEmptyClassroomDetailFragment :
    TransparentBottomSheetDialog<LiteRoomActivityClassroomDetailBinding>() {

    private lateinit var listAdapter: LiteEmptyClassroomDetailAdapter
    private var times: List<String> = emptyList()
    private var occupied: Set<Int> = emptySet()
    private var roomName: String = ""
    private var timetableName: String? = null
    private var week: Int = 1
    private var currentPeriod: Int = 1

    private fun refreshList(dow: Int) {
        val rows = times.mapIndexed { index, time ->
            val period = index + 1
            val isOccupied = occupied.contains(dow * 100 + period)
            LiteClassroomPeriodRow(
                number = getString(R.string.number_schedule, period),
                time = time,
                state = getString(
                    if (isOccupied) R.string.lite_room_detail_occupied
                    else R.string.lite_room_detail_free
                ),
                free = !isOccupied
            )
        }
        listAdapter.notifyDataSetChanged(rows)
        binding.list.scheduleLayoutAnimation()
    }

    override fun onStart() {
        super.onStart()
        val dow = TimeTools.getDow(System.currentTimeMillis())
        binding.tabs.getTabAt(dow - 1)?.let { binding.tabs.selectTab(it) }
        refreshList(dow)
        binding.list.scrollToPosition((currentPeriod - 1).coerceAtLeast(0))
    }

    override fun initViewBinding(v: View): LiteRoomActivityClassroomDetailBinding =
        LiteRoomActivityClassroomDetailBinding.bind(v)

    override fun getLayoutId(): Int = R.layout.lite_room_activity_classroom_detail

    @SuppressLint("SetTextI18n")
    override fun initViews(v: View) {
        val args = arguments
        roomName = args?.getString(ARG_ROOM_NAME).orEmpty()
        timetableName = args?.getString(ARG_TIMETABLE_NAME)
        week = args?.getInt(ARG_WEEK, 1) ?: 1
        currentPeriod = args?.getInt(ARG_CURRENT_PERIOD, 1) ?: 1
        times = args?.getStringArrayList(ARG_TIMES).orEmpty()
        occupied = args?.getIntArray(ARG_OCCUPIED)?.toSet().orEmpty()

        binding.subtitle.text =
            (timetableName ?: "") + getString(R.string.week_title, week)
        binding.title.text = getString(R.string.empty_classroom_format, roomName)
        listAdapter = LiteEmptyClassroomDetailAdapter(requireContext(), mutableListOf())
        binding.list.adapter = listAdapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        val dows = resources.getStringArray(R.array.dow2)
        for (i in 0 until 7) {
            binding.tabs.addTab(binding.tabs.newTab().setText(dows[i]))
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                refreshList(tab.position + 1)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    companion object {
        private const val ARG_ROOM_NAME = "lite_room_name"
        private const val ARG_TIMETABLE_NAME = "lite_room_timetable_name"
        private const val ARG_WEEK = "lite_room_week"
        private const val ARG_TIMES = "lite_room_times"
        private const val ARG_OCCUPIED = "lite_room_occupied"
        private const val ARG_CURRENT_PERIOD = "lite_room_current_period"

        /**
         * 前置条件：structure 与 room.occupied 来自同一次查询。
         * 后置条件：Fragment 只依赖 arguments 重建，参数里的时段编码为 day * 100 + period。
         */
        fun newInstance(
            room: ClassroomRoom,
            timetableName: String?,
            week: Int,
            structure: List<TimePeriodInDay>
        ): LiteEmptyClassroomDetailFragment {
            val fragment = LiteEmptyClassroomDetailFragment()
            val args = Bundle()
            args.putString(ARG_ROOM_NAME, room.name)
            args.putString(ARG_TIMETABLE_NAME, timetableName)
            args.putInt(ARG_WEEK, week)
            args.putStringArrayList(
                ARG_TIMES,
                ArrayList(structure.map { "${it.from} - ${it.to}" })
            )
            args.putIntArray(
                ARG_OCCUPIED,
                room.occupied.map { it.dayOfWeek * 100 + it.period }.distinct().toIntArray()
            )
            args.putInt(
                ARG_CURRENT_PERIOD,
                if (structure.isEmpty()) 1
                else (TimeTools.getCurrentScheduleNumber(structure) / 10).coerceAtLeast(1)
            )
            fragment.arguments = args
            return fragment
        }
    }
}
