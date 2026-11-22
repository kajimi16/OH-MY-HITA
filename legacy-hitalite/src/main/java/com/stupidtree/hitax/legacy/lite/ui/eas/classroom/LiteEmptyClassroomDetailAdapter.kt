package com.stupidtree.hitax.legacy.lite.ui.eas.classroom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteRoomActivityClassroomDetailItemBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder

@SuppressLint("ParcelCreator")
class LiteEmptyClassroomDetailAdapter(
    context: Context,
    beans: MutableList<LiteClassroomPeriodRow>
) : BaseListAdapter<LiteClassroomPeriodRow, LiteEmptyClassroomDetailAdapter.RowHolder>(
    context, beans
) {

    class RowHolder(binding: LiteRoomActivityClassroomDetailItemBinding) :
        BaseViewHolder<LiteRoomActivityClassroomDetailItemBinding>(binding)

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        LiteRoomActivityClassroomDetailItemBinding.inflate(mInflater, parent, false)

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): RowHolder =
        RowHolder(viewBinding as LiteRoomActivityClassroomDetailItemBinding)

    override fun bindHolder(holder: RowHolder, data: LiteClassroomPeriodRow?, position: Int) {
        val row = data ?: return
        holder.binding.number.text = row.number
        holder.binding.time.text = row.time
        holder.binding.state.text = row.state
        val primary = (mContext as? BaseActivity<*, *>)?.getColorPrimary()
        if (row.free && primary != null) {
            holder.binding.state.setTextColor(primary)
            holder.binding.state.setBackgroundResource(R.drawable.lite_element_round_primary)
        } else {
            holder.binding.state.setTextColor(Color.GRAY)
            holder.binding.state.setBackgroundResource(R.drawable.lite_room_round_grey)
        }
    }
}
