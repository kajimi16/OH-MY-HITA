package com.stupidtree.hitax.legacy.lite.ui.eas.imp

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteImportActivityImportItemBinding
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder

@SuppressLint("ParcelCreator")
class LiteImportStructureListAdapter(
    context: Context,
    beans: MutableList<TimePeriodInDay>
) : BaseListAdapter<TimePeriodInDay, LiteImportStructureListAdapter.PeriodHolder>(
    context, beans
) {

    class PeriodHolder(binding: LiteImportActivityImportItemBinding) :
        BaseViewHolder<LiteImportActivityImportItemBinding>(binding)

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        LiteImportActivityImportItemBinding.inflate(mInflater, parent, false)

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): PeriodHolder =
        PeriodHolder(viewBinding as LiteImportActivityImportItemBinding)

    override fun bindHolder(holder: PeriodHolder, data: TimePeriodInDay?, position: Int) {
        holder.binding.divider2.visibility =
            if (position == mBeans.size - 1) View.GONE else View.VISIBLE
        holder.binding.item.setOnClickListener {
            data?.let { period -> mOnItemClickListener?.onItemClick(period, it, position) }
        }
        holder.binding.title.text =
            mContext.getString(R.string.schedule_list_item_pattern, position + 1)
        data?.let { holder.binding.subtitle.text = it.toString() }
    }
}
