package com.stupidtree.hitax.legacy.lite.ui.timeline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TimeInDay
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteTimelineFragmentBinding
import com.stupidtree.hitax.utils.TextTools
import com.stupidtree.hitax.utils.TimeTools
import java.util.Calendar

class FragmentTimeLine : Fragment() {

    private var _binding: LiteTimelineFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FragmentTimelineViewModel
    private lateinit var listAdapter: TimelineListAdapter
    private lateinit var topListAdapter: TimelineTopListAdapter

    private var receiverRegistered = false

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED -> viewModel.refresh()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = LiteTimelineFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(FragmentTimelineViewModel::class.java)
        initListAndAdapter()
        observeData()
        viewModel.refresh()
    }

    private fun initListAndAdapter() {
        listAdapter = TimelineListAdapter(requireContext()).apply {
            onItemClick = { showEventDetail(it) }
        }
        topListAdapter = TimelineTopListAdapter(requireContext()).apply {
            onItemClick = { showEventDetail(it) }
        }
        binding.liteTimelineList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
            setHasFixedSize(false)
        }
        binding.liteTimelineUpcoming.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = topListAdapter
        }
    }

    private fun observeData() {
        viewModel.todayEvents.observe(viewLifecycleOwner) { events ->
            listAdapter.submitList(events)
            binding.liteTimelineEmpty.root.visibility =
                if (events.isEmpty()) View.VISIBLE else View.GONE
            binding.liteTimelineList.visibility =
                if (events.isEmpty()) View.GONE else View.VISIBLE
            updateHeader(events)
        }
        viewModel.upcomingEvents.observe(viewLifecycleOwner) { events ->
            topListAdapter.submitList(events)
            binding.liteTimelineUpcoming.visibility =
                if (events.isEmpty()) View.GONE else View.VISIBLE
            binding.liteTimelineUpcomingEmpty.visibility =
                if (events.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateHeader(events: List<EventItem>) {
        val header = binding.liteTimelineHeader
        val now = System.currentTimeMillis()
        val nowEvent = events.firstOrNull { it.containsTimeStamp(now) }
        val nextEvent = events.firstOrNull { it.happensAfterTimeStamp(now) }
        val courseCount = events.count { it.type == EventItem.TYPE.CLASS }
        val current = TimeInDay(Calendar.getInstance())
        val title: String
        val subtitle: String

        if (events.isEmpty()) {
            title = getString(R.string.lite_timeline_head_free_title)
            subtitle = getString(R.string.lite_timeline_head_free_subtitle)
        } else if (nowEvent != null) {
            title = nowEvent.name
            subtitle = getString(R.string.lite_timeline_head_ongoing_subtitle)
        } else {
            when {
                current < TimeInDay(5, 0) && current > TimeInDay(0, 0) -> {
                    title = getString(R.string.lite_timeline_head_goodnight_title)
                    subtitle = getString(R.string.lite_timeline_head_goodnight_subtitle)
                }
                current < TimeInDay(8, 15) && current > TimeInDay(5, 0) -> {
                    title = getString(R.string.lite_timeline_head_goodmorning_title)
                    subtitle = getString(
                        R.string.lite_timeline_head_goodmorning_subtitle, courseCount
                    )
                }
                current > TimeInDay(12, 15) && current < TimeInDay(13, 0) -> {
                    title = getString(R.string.lite_timeline_head_lunch_title)
                    subtitle = getString(R.string.lite_timeline_head_lunch_subtitle)
                }
                current > TimeInDay(17, 10) && current < TimeInDay(18, 10) -> {
                    title = getString(R.string.lite_timeline_head_dinner_title)
                    subtitle = getString(R.string.lite_timeline_head_dinner_subtitle)
                }
                nextEvent != null && nextEvent.getFromTimeDistance() <= 15 &&
                    (nextEvent.type == EventItem.TYPE.CLASS ||
                        nextEvent.type == EventItem.TYPE.EXAM) -> {
                    title = nextEvent.name
                    subtitle = getString(
                        R.string.lite_timeline_gonow_subtitle,
                        nextEvent.getFromTimeDistance()
                    )
                }
                nextEvent != null -> {
                    title = getString(R.string.lite_timeline_head_normal_title)
                    subtitle = getString(R.string.lite_timeline_head_normal_subtitle)
                }
                current > TimeInDay(23, 0) || current < TimeInDay(5, 0) -> {
                    title = getString(R.string.lite_timeline_head_goodnight_title)
                    subtitle = getString(R.string.lite_timeline_head_goodnight_subtitle)
                }
                else -> {
                    title = getString(R.string.lite_timeline_head_finish_title)
                    subtitle = getString(R.string.lite_timeline_head_finish_subtitle)
                }
            }
        }

        header.liteTimelineHeaderTitle.text = title
        header.liteTimelineHeaderSubtitle.text = subtitle
        header.liteTimelineHeaderDate.text = TimeTools.getDateString(
            requireContext(), Calendar.getInstance(), true, TimeTools.TTY_WK_FOLLOWING
        )
        header.liteTimelineHeaderProgress.visibility =
            if (nowEvent != null) View.VISIBLE else View.GONE
        header.liteTimelineHeaderProgress.progress =
            ((nowEvent?.getProgress(now) ?: 0f) * 100).toInt()
        header.liteTimelineHeaderCounting.text = if (nextEvent != null) {
            getString(
                R.string.lite_timeline_counting_pattern,
                nextEvent.name,
                countingText(nextEvent.getFromTimeDistance())
            )
        } else {
            getString(R.string.lite_timeline_counting_free)
        }
    }

    private fun countingText(minutes: Int): String = if (minutes >= 60) {
        getString(R.string.lite_timeline_counting_hour, minutes / 60, minutes % 60)
    } else {
        getString(R.string.lite_timeline_counting_minute, minutes)
    }

    private fun showEventDetail(event: EventItem) {
        val time = TextTools.getChatTimeText(requireContext(), event.from) +
            " - " + TextTools.getChatTimeText(requireContext(), event.to)
        val place = event.place?.takeIf { it.isNotBlank() }
            ?: getString(R.string.lite_timeline_unknown_location)
        AlertDialog.Builder(requireContext())
            .setTitle(event.name)
            .setMessage(
                getString(R.string.lite_timeline_event_time, time) + "\n" +
                    getString(R.string.lite_timeline_event_place, place)
            )
            .setPositiveButton(R.string.lite_timeline_close, null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
            }
            requireContext().registerReceiver(timeReceiver, filter)
            receiverRegistered = true
        }
    }

    override fun onStop() {
        if (receiverRegistered) {
            requireContext().unregisterReceiver(timeReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onDestroyView() {
        binding.liteTimelineList.adapter = null
        binding.liteTimelineUpcoming.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
