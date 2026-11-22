package com.stupidtree.hitax.legacy.lite.ui.event

import android.os.Bundle
import android.view.View
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TimeInDay
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteEventDialogClassBinding
import com.stupidtree.hitax.utils.TimeTools
import com.stupidtree.style.base.BaseFragment
import com.stupidtree.style.widgets.PopUpText
import java.util.Calendar

/**
 * 事件详情内容页。
 *
 * 前置条件：arguments 中带有 Serializable 的 EventItem。
 * 后置条件：展示教师、地点、时间、节数、日期与课程进度。
 * 违约处理：缺少事件参数时保持空白，不抛出异常。
 */
class LiteEventItemFragment :
    BaseFragment<LiteEventItemViewModel, LiteEventDialogClassBinding>() {

    interface EventParent {
        fun callDismiss()
    }
    private fun setInfo(eventItem: EventItem) {
        binding?.teacher?.text = displayText(eventItem.teacher)
        binding?.place?.text = displayText(eventItem.place)
        binding?.time?.text = getString(
            R.string.lite_event_duration_text,
            TimeInDay(eventItem.from).toString(),
            TimeInDay(eventItem.to).toString(),
        )
        binding?.ttDlgName?.text = eventItem.name
        val numberString =
            (eventItem.fromNumber until eventItem.fromNumber + eventItem.lastNumber)
                .joinToString(separator = ", ")
        binding?.timeNumber?.text = numberString
        binding?.numberLayout?.visibility =
            if (numberString.isEmpty()) View.GONE else View.VISIBLE
        binding?.ttDlgValue3Detail?.setOnClickListener {
            LiteHostBridge.resourceUi?.openSearch(requireContext(), eventItem.teacher, true)
        }
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = eventItem.from.time
        binding?.date?.text =
            TimeTools.getDateString(requireContext(), calendar, false, TimeTools.TTY_FOLLOWING)
    }
    private fun displayText(value: String?): String =
        if (value.isNullOrEmpty()) getString(R.string.lite_event_none) else value

    override fun initViews(view: View) {
        viewModel.eventItemLiveData.value =
            arguments?.getSerializable(ARG_EVENT) as? EventItem
        viewModel.eventItemLiveData.observe(this) { event ->
            event?.let { setInfo(it) }
        }
        viewModel.progressLiveData.observe(this) { progress ->
            val total = progress.second.coerceAtLeast(1)
            binding?.courseProgress?.progress =
                (((progress.first + 1).toFloat() / total) * 100).toInt()
            binding?.courseCourseInSubject?.text =
                getString(R.string.lite_event_dialog_this_course_p, progress.first + 1)
        }
        binding?.subject?.setOnClickListener {
            viewModel.eventItemLiveData.value?.let { event ->
                LiteHostBridge.timetableUi?.openSubject(requireContext(), event.subjectId)
            }
        }
        binding?.nameLayout?.setOnClickListener { binding?.subject?.callOnClick() }
        binding?.delete?.setOnClickListener {
            PopUpText()
                .setTitle(R.string.lite_event_delete_confirm)
                .setOnConfirmListener(object : PopUpText.OnConfirmListener {
                    override fun OnConfirm() {
                        viewModel.delete()
                        dismissParent()
                    }
                })
                .show(childFragmentManager, "lite_event_delete")
        }
    }

    private fun dismissParent() {
        val parent = parentFragment
        if (parent is EventParent) {
            parent.callDismiss()
        } else {
            (activity as? EventParent)?.callDismiss()
        }
    }

    override fun getViewModelClass(): Class<LiteEventItemViewModel> =
        LiteEventItemViewModel::class.java

    override fun initViewBinding(): LiteEventDialogClassBinding =
        LiteEventDialogClassBinding.inflate(layoutInflater)

    companion object {
        private const val ARG_EVENT = "event"

        fun newInstance(eventItem: EventItem): LiteEventItemFragment =
            LiteEventItemFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_EVENT, eventItem) }
            }
    }
}
