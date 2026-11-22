package com.stupidtree.hitax.legacy.lite.ui.eas.score

import android.os.Bundle
import android.view.View
import com.stupidtree.hita.api.ScoreEntry
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteScoreDialogDetailBinding
import com.stupidtree.style.widgets.TransparentBottomSheetDialog

/**
 * 成绩详情底部弹窗。
 *
 * 只展示宿主 ScoreService 提供的字段：课程、学分、成绩、学期。
 * 所有参数放在 arguments 里，重建 Fragment 时不依赖构造参数。
 */
class LiteScoreDetailFragment :
    TransparentBottomSheetDialog<LiteScoreDialogDetailBinding>() {

    override fun getLayoutId(): Int = R.layout.lite_score_dialog_detail

    override fun initViewBinding(v: View): LiteScoreDialogDetailBinding =
        LiteScoreDialogDetailBinding.bind(v)

    override fun initViews(v: View) {
        val args = arguments ?: return
        binding.title.text = args.getString(ARG_COURSE_NAME).orEmpty()
        binding.scoreCredits.text = args.getInt(ARG_CREDITS, 0).toString()
        binding.scoreValue.text = args.getInt(ARG_SCORE, 0).toString()
        binding.scoreTerm.text = args.getString(ARG_TERM_NAME).orEmpty()
    }

    companion object {
        private const val ARG_COURSE_NAME = "course_name"
        private const val ARG_CREDITS = "credits"
        private const val ARG_SCORE = "score"
        private const val ARG_TERM_NAME = "term_name"

        fun newInstance(entry: ScoreEntry): LiteScoreDetailFragment =
            LiteScoreDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COURSE_NAME, entry.courseName)
                    putInt(ARG_CREDITS, entry.credits)
                    putInt(ARG_SCORE, entry.score)
                    putString(ARG_TERM_NAME, entry.termName)
                }
            }
    }
}
