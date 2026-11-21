package com.stupidtree.hitax.legacy.lite.ui.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteDynamicTimelineCardBinding
import com.stupidtree.hitax.utils.TextTools
import com.stupidtree.hitax.utils.TimeTools

class TimelineListAdapter(private val context: Context) :
    RecyclerView.Adapter<TimelineListAdapter.EventHolder>() {

    private val items = mutableListOf<EventItem>()
    private var nowEvent: EventItem? = null
    private var nowProgress = 0f

    var onItemClick: ((EventItem) -> Unit)? = null

    fun submitList(newItems: List<EventItem>) {
        items.clear()
        items.addAll(newItems)
        val now = System.currentTimeMillis()
        nowEvent = items.firstOrNull { it.containsTimeStamp(now) }
        nowProgress = nowEvent?.getProgress(now) ?: 0f
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        val binding = LiteDynamicTimelineCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventHolder(binding)
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class EventHolder(private val binding: LiteDynamicTimelineCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: EventItem, position: Int) {
            val passed = TimeTools.passed(event.to)
            binding.liteTimelineName.text = event.name
            binding.liteTimelineTime.text = TextTools.getChatTimeText(context, event.from) +
                "-" + TextTools.getChatTimeText(context, event.to)
            binding.liteTimelinePlace.text = event.place?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.lite_timeline_unknown_location)
            binding.liteTimelineDuration.text = durationText(event.getDurationInMinutes())
            binding.liteTimelineDot.setBackgroundResource(
                if (passed) R.drawable.lite_timeline_dot_passed else R.drawable.lite_timeline_dot
            )
            binding.liteTimelineCard.setBackgroundResource(
                if (passed) R.drawable.lite_timeline_bg_card_passed
                else R.drawable.lite_timeline_bg_card
            )
            val accent = ContextCompat.getColor(context, R.color.lite_timeline_accent)
            binding.liteTimelineName.setTextColor(
                if (!passed && event.type != EventItem.TYPE.OTHER) accent
                else ContextCompat.getColor(context, R.color.lite_timeline_text_primary)
            )
            val alpha = if (passed) 0.6f else 1f
            binding.liteTimelineName.alpha = alpha
            binding.liteTimelineTime.alpha = alpha
            binding.liteTimelinePlace.alpha = alpha
            binding.liteTimelineDuration.alpha = alpha
            binding.liteTimelineRailLine.visibility =
                if (position == items.size - 1) android.view.View.INVISIBLE else android.view.View.VISIBLE
            val isNow = event == nowEvent
            binding.liteTimelineProgress.visibility =
                if (isNow) android.view.View.VISIBLE else android.view.View.GONE
            binding.liteTimelineProgress.progress = (nowProgress * 100).toInt()
            binding.liteTimelineCard.setOnClickListener { onItemClick?.invoke(event) }
        }

        private fun durationText(minutes: Int): String {
            if (minutes < 60) return context.getString(R.string.lite_timeline_duration_minute, minutes)
            val hours = minutes / 60
            val rest = minutes % 60
            return if (rest == 0) context.getString(R.string.lite_timeline_duration_hour, hours)
            else context.getString(R.string.lite_timeline_duration_hour_minute, hours, rest)
        }
    }
}
