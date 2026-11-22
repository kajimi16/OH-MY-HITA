package com.stupidtree.hitax.legacy.lite.ui.event

import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteEventDialogEventsBinding
import com.stupidtree.style.widgets.TransparentBottomSheetDialog
import java.util.ArrayList

/**
 * 同一天多个事件的底部弹窗。
 *
 * 前置条件：arguments 携带 Serializable 的 ArrayList<EventItem>。
 * 后置条件：单事件直接展示详情；多事件以 Tab 切换详情。
 * 违约处理：事件列表为空时关闭弹窗。
 */
class LiteEventTimeInfoSheet :
    TransparentBottomSheetDialog<LiteEventDialogEventsBinding>(),
    LiteEventItemFragment.EventParent {

    private var currentPosition = 0
    private val events: MutableList<EventItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serialized = arguments?.getSerializable(ARG_EVENTS) as? List<*>
        serialized?.forEach { item ->
            (item as? EventItem)?.let { events.add(it) }
        }
    }

    override fun initViews(view: View) {
        if (events.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val animationSet =
                    if (currentPosition < tab.position) {
                        R.anim.lite_event_fragment_slide_from_right to
                            R.anim.lite_event_fragment_slide_to_left
                    } else {
                        R.anim.lite_event_fragment_slide_from_left to
                            R.anim.lite_event_fragment_slide_to_right
                    }
                childFragmentManager.beginTransaction()
                    .setCustomAnimations(animationSet.first, animationSet.second)
                    .replace(
                        R.id.layout,
                        LiteEventItemFragment.newInstance(events[tab.position]),
                    )
                    .commitAllowingStateLoss()
                currentPosition = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        if (events.size <= 1) {
            binding.tabs.visibility = View.GONE
            binding.layout.visibility = View.VISIBLE
            childFragmentManager.beginTransaction()
                .add(R.id.layout, LiteEventItemFragment.newInstance(events[0]), "event")
                .commit()
        } else {
            binding.layout.visibility = View.VISIBLE
            binding.tabs.visibility = View.VISIBLE
            binding.tabs.removeAllTabs()
            events.forEach { binding.tabs.addTab(binding.tabs.newTab().setText(it.name)) }
            childFragmentManager.beginTransaction()
                .replace(R.id.layout, LiteEventItemFragment.newInstance(events[0]), "event")
                .commit()
            currentPosition = 0
        }
    }

    override fun getLayoutId(): Int = R.layout.lite_event_dialog_events

    override fun initViewBinding(view: View): LiteEventDialogEventsBinding =
        LiteEventDialogEventsBinding.bind(view)

    override fun callDismiss() {
        dismiss()
    }

    companion object {
        private const val ARG_EVENTS = "events"

        fun newInstance(events: ArrayList<EventItem>): LiteEventTimeInfoSheet =
            LiteEventTimeInfoSheet().apply {
                arguments = Bundle().apply { putSerializable(ARG_EVENTS, events) }
            }
    }
}
