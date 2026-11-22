package com.stupidtree.hitax.legacy.lite.ui.eas.classroom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import com.stupidtree.hita.api.ClassroomRoom
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteRoomActivityClassroomItemBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder
import com.stupidtree.hitax.utils.TimeTools

/**
 * 空教室网格。
 *
 * 每间教室的占用状态由宿主服务给出的占用时段与当前课表结构推导：
 * 当前节被占显示「被占」，下一节被占显示「将占」，其余显示「空闲」。
 */
@SuppressLint("ParcelCreator")
class LiteEmptyClassroomListAdapter(
    context: Context,
    beans: MutableList<ClassroomRoom>,
    private val structureProvider: () -> List<TimePeriodInDay>
) : BaseListAdapter<ClassroomRoom, LiteEmptyClassroomListAdapter.RoomHolder>(context, beans) {

    class RoomHolder(binding: LiteRoomActivityClassroomItemBinding) :
        BaseViewHolder<LiteRoomActivityClassroomItemBinding>(binding)

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        LiteRoomActivityClassroomItemBinding.inflate(mInflater, parent, false)

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): RoomHolder =
        RoomHolder(viewBinding as LiteRoomActivityClassroomItemBinding)

    override fun bindHolder(holder: RoomHolder, data: ClassroomRoom?, position: Int) {
        val room = data ?: return
        holder.binding.name.text = room.name
        holder.binding.capacity.text = room.capacity.toString()
        val state = stateOf(room)
        holder.binding.state.setText(state.textRes)
        val primary = (mContext as? BaseActivity<*, *>)?.getColorPrimary()
        if (state == State.FREE && primary != null) {
            holder.binding.state.setBackgroundResource(R.drawable.element_rounded_button_bg_primary_light)
            holder.binding.state.setTextColor(primary)
        } else {
            holder.binding.state.setBackgroundResource(R.drawable.element_rounded_button_bg_grey)
            holder.binding.state.setTextColor(Color.GRAY)
        }
        holder.binding.card.setOnClickListener { view ->
            mOnItemClickListener?.onItemClick(room, view, position)
        }
    }

    private fun stateOf(room: ClassroomRoom): State {
        val structure = structureProvider()
        if (structure.isEmpty()) return State.UNKNOWN
        val raw = TimeTools.getCurrentScheduleNumber(structure)
        val period = when {
            raw <= 0 -> return State.UNKNOWN
            raw % 10 == 5 -> raw / 10 + 1
            else -> raw / 10
        }
        val dow = TimeTools.getDow(System.currentTimeMillis())
        return when {
            room.occupied.any { it.dayOfWeek == dow && it.period == period } -> State.OCCUPIED
            room.occupied.any { it.dayOfWeek == dow && it.period == period + 1 } -> State.SOON
            else -> State.FREE
        }
    }

    private enum class State(@StringRes val textRes: Int) {
        FREE(R.string.lite_room_state_free),
        SOON(R.string.lite_room_state_soon),
        OCCUPIED(R.string.lite_room_state_occupied),
        UNKNOWN(R.string.lite_room_state_unknown),
    }
}
