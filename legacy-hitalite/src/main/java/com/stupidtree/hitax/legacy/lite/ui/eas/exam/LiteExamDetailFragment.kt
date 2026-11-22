package com.stupidtree.hitax.legacy.lite.ui.eas.exam

import android.os.Bundle
import android.view.View
import com.stupidtree.hitax.data.model.eas.ExamItem
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteExamDialogDetailBinding
import com.stupidtree.style.widgets.TransparentBottomSheetDialog

/**
 * 考试详情底部弹窗。
 *
 * 展示宿主 ExamItem 的全部可读字段。
 * 所有参数放在 arguments 里，重建 Fragment 时不依赖构造参数。
 */
class LiteExamDetailFragment :
    TransparentBottomSheetDialog<LiteExamDialogDetailBinding>() {

    override fun getLayoutId(): Int = R.layout.lite_exam_dialog_detail

    override fun initViewBinding(v: View): LiteExamDialogDetailBinding =
        LiteExamDialogDetailBinding.bind(v)

    override fun initViews(v: View) {
        val args = arguments ?: return
        binding.title.text = args.getString(ARG_COURSE_NAME).orEmpty()
        binding.examCampus.text = args.getString(ARG_CAMPUS_NAME).orEmpty()
        binding.examLocation.text = args.getString(ARG_LOCATION).orEmpty()
        binding.examTerm.text = args.getString(ARG_TERM_NAME).orEmpty()
        binding.examTime.text = args.getString(ARG_EXAM_TIME).orEmpty()
        binding.examType.text = args.getString(ARG_EXAM_TYPE).orEmpty()
    }

    companion object {
        private const val ARG_COURSE_NAME = "course_name"
        private const val ARG_CAMPUS_NAME = "campus_name"
        private const val ARG_LOCATION = "exam_location"
        private const val ARG_TERM_NAME = "term_name"
        private const val ARG_EXAM_TIME = "exam_time"
        private const val ARG_EXAM_TYPE = "exam_type"

        fun newInstance(exam: ExamItem): LiteExamDetailFragment =
            LiteExamDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COURSE_NAME, exam.courseName)
                    putString(ARG_CAMPUS_NAME, exam.campusName)
                    putString(ARG_LOCATION, exam.examLocation)
                    putString(ARG_TERM_NAME, exam.termName)
                    putString(ARG_EXAM_TIME, exam.examTime)
                    putString(ARG_EXAM_TYPE, exam.examType)
                }
            }
    }
}
