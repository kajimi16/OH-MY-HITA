package com.stupidtree.hitax.legacy.lite.ui.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteDynamicTimelineTopBinding
import com.stupidtree.hitax.utils.TimeTools

class TimelineTopListAdapter(private val context: Context) :
    RecyclerView.Adapter<TimelineTopListAdapter.TopHolder>() {

    private val items = mutableListOf<EventItem>()

    var onItemClick: ((EventItem) -> Unit)? = null

    fun submitList(newItems: List<EventItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopHolder {
        val binding = LiteDynamicTimelineTopBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TopHolder(binding)
    }

    override fun onBindViewHolder(holder: TopHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TopHolder(private val binding: LiteDynamicTimelineTopBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: EventItem) {
            binding.liteTimelineTopName.text = event.name
            val from = event.from.time
            binding.liteTimelineTopTime.text = if (TimeTools.isSameDay(
                    System.currentTimeMillis(), from
                )
            ) {
                val minutes = event.getFromTimeDistance()
                if (minutes >= 60) context.getString(
                    R.string.lite_timeline_top_countdown_hour, minutes / 60, minutes % 60
                ) else context.getString(R.string.lite_timeline_top_countdown_minute, minutes)
            } else {
                TimeTools.getDateString(context, from, true, TimeTools.TTY_REPLACE)
            }
            binding.liteTimelineTopCard.setOnClickListener { onItemClick?.invoke(event) }
        }
    }
}
