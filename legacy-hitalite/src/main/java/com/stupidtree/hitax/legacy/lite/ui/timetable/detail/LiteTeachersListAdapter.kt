package com.stupidtree.hitax.legacy.lite.ui.timetable.detail

import android.content.Context
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.legacy.lite.databinding.LiteDetailTeacherItemBinding
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder

/**
 * 课表详情中的教师横向列表。
 *
 * 前置条件：数据项来自宿主 TimetableService.getTeachersInfo。
 * 后置条件：点击项通过 ItemClickListener 交给页面处理。
 */
class LiteTeachersListAdapter(
    context: Context,
    teachers: MutableList<TeacherInfo>,
) : BaseListAdapter<TeacherInfo, LiteTeachersListAdapter.TeacherHolder>(context, teachers) {

    class TeacherHolder(binding: LiteDetailTeacherItemBinding) :
        BaseViewHolder<LiteDetailTeacherItemBinding>(binding)

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        LiteDetailTeacherItemBinding.inflate(mInflater, parent, false)

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): TeacherHolder =
        TeacherHolder(viewBinding as LiteDetailTeacherItemBinding)

    override fun bindHolder(holder: TeacherHolder, data: TeacherInfo?, position: Int) {
        holder.binding.teacherSubject.text = data?.subjectName
        holder.binding.teacherName.text = data?.name
        holder.binding.teacherCard.setOnClickListener { view ->
            mOnItemClickListener?.onItemClick(data, view, position)
        }
    }
}
