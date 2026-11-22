package com.stupidtree.hitax.legacy.lite.ui.timetable.detail

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteDetailSubjectListTitleBinding
import com.stupidtree.hitax.legacy.lite.databinding.LiteDetailSubjectsItemBinding
import com.stupidtree.style.base.BaseCheckableListAdapter
import com.stupidtree.style.base.BaseViewHolder

/**
 * 课表详情中的科目列表。
 *
 * 前置条件：数据项来自宿主 TimetableService.getSubjects。
 * 后置条件：点击颜色块回调 [OnColorClickListener]；长按进入编辑模式。
 * 违约处理：进度总数不大于 0 时进度条按 0 显示。
 */
class LiteSubjectsListAdapter(
    context: Context,
    subjects: MutableList<TermSubject>,
    private val viewModel: LiteTimetableDetailViewModel,
    private val lifecycleOwner: LifecycleOwner,
) : BaseCheckableListAdapter<TermSubject, LiteSubjectsListAdapter.SubjectViewHolder>(
    context, subjects
) {

    interface OnColorClickListener {
        fun onColorClick(data: TermSubject)
    }

    var onColorClickListener: OnColorClickListener? = null

    override fun getItemViewType(position: Int): Int =
        if (mBeans[position].type == TermSubject.TYPE.TAG) TITLE else NORMAL

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        if (viewType == NORMAL) {
            LiteDetailSubjectsItemBinding.inflate(mInflater, parent, false)
        } else {
            LiteDetailSubjectListTitleBinding.inflate(mInflater, parent, false)
        }

    class SubjectViewHolder(binding: ViewBinding) :
        BaseViewHolder<ViewBinding>(binding), CheckableViewHolder {

        override fun showCheckBox() {
            (binding as? LiteDetailSubjectsItemBinding)?.check?.visibility = View.VISIBLE
        }

        override fun hideCheckBox() {
            (binding as? LiteDetailSubjectsItemBinding)?.check?.visibility = View.GONE
        }

        override fun toggleCheck() {
            (binding as? LiteDetailSubjectsItemBinding)?.check?.toggle()
        }

        override fun setChecked(boolean: Boolean) {
            (binding as? LiteDetailSubjectsItemBinding)?.check?.isChecked = boolean
        }

        override fun setInternalOnLongClickListener(listener: View.OnLongClickListener) {
            (binding as? LiteDetailSubjectsItemBinding)?.item?.setOnLongClickListener(listener)
        }

        override fun setInternalOnClickListener(listener: View.OnClickListener) {
            (binding as? LiteDetailSubjectsItemBinding)?.item?.setOnClickListener(listener)
        }

        override fun setInternalOnCheckedChangeListener(
            listener: CompoundButton.OnCheckedChangeListener,
        ) {
            (binding as? LiteDetailSubjectsItemBinding)?.check?.setOnCheckedChangeListener(listener)
        }
    }

    override fun bindHolder(holder: SubjectViewHolder, data: TermSubject?, position: Int) {
        super.bindHolder(holder, data, position)
        when (val binding = holder.binding) {
            is LiteDetailSubjectListTitleBinding -> {
                binding.name.text = mBeans[position].name
            }

            is LiteDetailSubjectsItemBinding -> {
                binding.name.text = data?.name
                if (data?.color != null) {
                    binding.icon.setColorFilter(data.color)
                } else {
                    binding.icon.clearColorFilter()
                }
                data?.id?.let { subjectId ->
                    viewModel.getSubjectProgress(subjectId).observe(lifecycleOwner) { progress ->
                        val total = progress.second.coerceAtLeast(1)
                        binding.progress.progress =
                            (progress.first.toFloat() / total * 100).toInt()
                    }
                }
                binding.icon.setOnClickListener {
                    data?.let { onColorClickListener?.onColorClick(it) }
                }
                binding.label.text =
                    if (TextUtils.isEmpty(data?.school)) {
                        mContext.getString(R.string.lite_detail_unknown_department)
                    } else {
                        data?.school
                    }
                binding.icon.visibility = if (isEditMode) View.GONE else View.VISIBLE
            }
        }
    }

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): SubjectViewHolder =
        SubjectViewHolder(viewBinding)

    companion object {
        private const val NORMAL = 967
        private const val TITLE = 971
    }
}
