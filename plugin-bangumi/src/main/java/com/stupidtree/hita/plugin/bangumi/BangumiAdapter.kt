package com.stupidtree.hita.plugin.bangumi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BangumiAdapter(
    private val onClick: (Anime) -> Unit,
) : RecyclerView.Adapter<BangumiAdapter.Holder>() {

    private val items = mutableListOf<Anime>()

    fun submit(list: List<Anime>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bangumi, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val anime = items[position]
        holder.name.text = anime.nameCn ?: anime.name
        holder.sub.text = buildString {
            if (!anime.nameCn.isNullOrBlank()) append(anime.name)
            if (anime.airWeekday != null) {
                if (isNotEmpty()) append(" · ")
                append("周").append(weekdayName(anime.airWeekday))
            }
        }.ifBlank { "番剧" }
        holder.score.text = anime.score?.let {
            holder.itemView.context.getString(R.string.bangumi_score, it)
        } ?: ""
        holder.score.visibility = if (anime.score == null) View.GONE else View.VISIBLE
        Glide.with(holder.cover)
            .load(anime.cover)
            .centerCrop()
            .into(holder.cover)
        holder.itemView.setOnClickListener { onClick(anime) }
    }

    private fun weekdayName(day: Int): String = when (day) {
        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.cover)
        val name: TextView = view.findViewById(R.id.name)
        val sub: TextView = view.findViewById(R.id.sub)
        val score: TextView = view.findViewById(R.id.score)
    }
}
