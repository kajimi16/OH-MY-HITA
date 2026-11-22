package com.stupidtree.hitax.legacy.lite.ui.eas.score

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.stupidtree.hita.api.ScoreEntry
import com.stupidtree.hitax.legacy.lite.databinding.LiteScoreItemBinding
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.base.BaseViewHolder

/**
 * HITA_Lite 成绩列表。
 *
 * 数据来自宿主 ScoreService.ScoreReport.entries，布局与 Lite 2.1 的成绩条目一致。
 */
@SuppressLint("ParcelCreator")
class LiteScoresListAdapter(context: Context, beans: MutableList<ScoreEntry>) :
    BaseListAdapter<ScoreEntry, LiteScoresListAdapter.SHolder>(context, beans) {

    class SHolder(viewBinding: LiteScoreItemBinding) :
        BaseViewHolder<LiteScoreItemBinding>(viewBinding)

    override fun bindHolder(holder: SHolder, data: ScoreEntry?, position: Int) {
        holder.binding.divider2.visibility =
            if (position == mBeans.size - 1) View.GONE else View.VISIBLE
        holder.binding.title.text = data?.courseName
        holder.binding.scores.text = data?.score?.toString()
        holder.binding.item.setOnClickListener { view ->
            mOnItemClickListener?.onItemClick(data, view, position)
        }
    }

    override fun createViewHolder(viewBinding: ViewBinding, viewType: Int): SHolder =
        SHolder(viewBinding as LiteScoreItemBinding)

    override fun getViewBinding(parent: ViewGroup, viewType: Int): ViewBinding =
        LiteScoreItemBinding.inflate(mInflater, parent, false)
}
