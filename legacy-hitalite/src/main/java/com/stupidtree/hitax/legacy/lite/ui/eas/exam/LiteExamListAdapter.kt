package com.stupidtree.hitax.legacy.lite.ui.eas.exam

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.stupidtree.hitax.data.model.eas.ExamItem
import com.stupidtree.hitax.legacy.lite.databinding.LiteExamItemBinding
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder

/**
 * HITA_Lite 考试列表。
 *
 * 数据来自宿主 ExamService；布局与 Lite 2.1 的考试条目一致。
 */
@SuppressLint("ParcelCreator")
class LiteExamListAdapter(context: Context, beans: MutableList<ExamItem>) :
    BaseListAdapter<ExamItem, LiteExamListAdapter.SHolder>(context, beans) {

    class SHolder(viewBinding: LiteExamItemBinding) :
        BaseViewHolder<LiteExamItemBinding>(viewBinding)

    override fun bindHolder(holder: SHolder, data: ExamItem?, position: Int) {
        holder.binding.divider2.visibility =
            if (position == mBeans.size - 1) View.GONE else View.VISIBLE
        holder.binding.title.text = data?.courseName
        holder.binding.time.text = data?.examDate
        holder.binding.item.setOnClickListener { view ->
            mOnItemClickListener?.onItemClick(data, view, position)
        }
    }

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): SHolder =
        SHolder(viewBinding as LiteExamItemBinding)

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        LiteExamItemBinding.inflate(mInflater, parent, false)
}
