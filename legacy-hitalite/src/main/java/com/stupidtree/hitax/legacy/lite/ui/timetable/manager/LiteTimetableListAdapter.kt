package com.stupidtree.hitax.legacy.lite.ui.timetable.manager

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteActivityTimetableManagerItemAddBinding
import com.stupidtree.hitax.legacy.lite.databinding.LiteActivityTimetableManagerItemBinding
import com.stupidtree.hitax.utils.TimeTools
import com.stupidtree.style.base.BaseCheckableListAdapter
import com.stupidtree.style.base.BaseViewHolder

/**
 * HITA_Lite 课表管理网格。
 *
 * 与 Lite 2.1 的 TimetableListAdapter 同布局；数据模型来自宿主 :model。
 */
@SuppressLint("ParcelCreator")
class LiteTimetableListAdapter(context: Context, mBeans: MutableList<Timetable>) :
    BaseCheckableListAdapter<Timetable, RecyclerView.ViewHolder>(context, mBeans) {

    enum class SOURCE { EAS, NEW }

    interface OnAddClickListener {
        fun onAddClick(source: SOURCE)
    }

    private var addClickListener: OnAddClickListener? = null

    fun setOnAddClickListener(listener: OnAddClickListener) {
        addClickListener = listener
    }

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        if (viewType == TYPE_ITEM) {
            LiteActivityTimetableManagerItemBinding.inflate(mInflater, parent, false)
        } else {
            LiteActivityTimetableManagerItemAddBinding.inflate(mInflater, parent, false)
        }

    override fun getItemViewType(position: Int): Int =
        if (position == mBeans.size) TYPE_ADD else TYPE_ITEM

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_ITEM) {
            Holder(viewBinding as LiteActivityTimetableManagerItemBinding)
        } else {
            AddHolder(viewBinding as LiteActivityTimetableManagerItemAddBinding)
        }

    @SuppressLint("SetTextI18n")
    override fun bindHolder(holder: RecyclerView.ViewHolder, data: Timetable?, position: Int) {
        super.bindHolder(holder, data, position)
        if (holder is Holder) {
            holder.binding.title.text = data?.name
            holder.binding.subtitle.text = TimeTools.printDate(data?.startTime?.time)
            data?.startTime?.time?.let {
                holder.binding.icon.setImageResource(
                    when (TimeTools.getSeason(it)) {
                        TimeTools.SEASON.SPRING -> R.drawable.lite_season_spring
                        TimeTools.SEASON.SUMMER -> R.drawable.lite_season_summer
                        TimeTools.SEASON.AUTUMN -> R.drawable.lite_season_autumn
                        else -> R.drawable.lite_season_winter
                    }
                )
                holder.binding.icon.setBackgroundResource(
                    when (TimeTools.getSeason(it)) {
                        TimeTools.SEASON.SPRING -> R.drawable.lite_element_round_spring
                        TimeTools.SEASON.SUMMER -> R.drawable.lite_element_round_summer
                        TimeTools.SEASON.AUTUMN -> R.drawable.lite_element_round_autumn
                        else -> R.drawable.lite_element_round_winter
                    }
                )
            }
        } else if (holder is AddHolder) {
            holder.binding.icon.setOnClickListener {
                addClickListener?.onAddClick(SOURCE.NEW)
            }
            holder.binding.eas.setOnClickListener {
                addClickListener?.onAddClick(SOURCE.EAS)
            }
        }
    }

    fun notifyDatasetChanged(list: List<Timetable>) {
        mBeans.clear()
        mBeans.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = super.getItemCount() + 1

    inner class Holder(itemView: LiteActivityTimetableManagerItemBinding) :
        BaseViewHolder<LiteActivityTimetableManagerItemBinding>(itemView), CheckableViewHolder {
        override fun showCheckBox() {
            binding.check.visibility = View.VISIBLE
        }

        override fun hideCheckBox() {
            binding.check.visibility = View.GONE
        }

        override fun toggleCheck() {
            binding.check.toggle()
        }

        override fun setChecked(boolean: Boolean) {
            binding.check.isChecked = boolean
        }

        override fun setInternalOnLongClickListener(listener: View.OnLongClickListener) {
            binding.card.setOnLongClickListener(listener)
        }

        override fun setInternalOnClickListener(listener: View.OnClickListener) {
            binding.card.setOnClickListener(listener)
        }

        override fun setInternalOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
            binding.check.setOnCheckedChangeListener(listener)
        }
    }

    inner class AddHolder(viewBinding: LiteActivityTimetableManagerItemAddBinding) :
        BaseViewHolder<LiteActivityTimetableManagerItemAddBinding>(viewBinding)

    companion object {
        const val TYPE_ADD = 301
        const val TYPE_ITEM = 300
    }
}
