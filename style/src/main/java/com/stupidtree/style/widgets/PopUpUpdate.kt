package com.stupidtree.style.widgets

import android.text.TextUtils
import android.view.View
import androidx.annotation.StringRes
import com.stupidtree.style.R
import com.stupidtree.style.databinding.DialogBottomTextBinding
import com.stupidtree.style.databinding.DialogBottomUpdateBinding

/**
 * 圆角的文本框底部弹窗
 */
class PopUpUpdate : TransparentBottomSheetDialog<DialogBottomUpdateBinding>() {


    @StringRes
    var init_title: Int? = null
    private var init_text: String? = null
    var onActionListener: OnActionListener? = null

    interface OnActionListener {
        fun onConfirm()
        fun onCancel()
        fun onSkip()
    }

    override fun onStart() {
        super.onStart()
        // 更新日志可能很长：让弹窗展开到可用高度，正文区域内部滚动，
        // 保证底部三个按钮始终在屏幕内。
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior?.apply {
            isFitToContents = true
            skipCollapsed = true
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        binding.text.requestFocus()
    }

    fun setTitle(@StringRes title: Int): PopUpUpdate {
        init_title = title
        return this
    }

    fun setDialogCancelable(cancelable: Boolean): PopUpUpdate {
        isCancelable = cancelable
        return this
    }

    fun setText(text: String?): PopUpUpdate {
        init_text = text
        return this
    }

    fun setOnActionListener(lis: OnActionListener): PopUpUpdate {
        this.onActionListener = lis
        return this
    }


    override fun initViews(v: View) {
        if (init_title != null) {
            binding.title.setText(init_title!!)
        }
        val markdown = init_text
        if (!markdown.isNullOrEmpty()) {
            // 更新日志来自 GitHub Release 正文，按 Markdown 渲染
            io.noties.markwon.Markwon.create(requireContext()).setMarkdown(binding.text, markdown)
            binding.text.visibility = View.VISIBLE
        } else {
            binding.text.visibility = View.GONE
        }
        if (isCancelable) {
            binding.cancel.visibility = View.VISIBLE
        } else {
            binding.cancel.visibility = View.GONE
        }
        binding.skip.setOnClickListener {
            onActionListener?.onSkip()
            dismiss()
        }
        binding.cancel.setOnClickListener {
            onActionListener?.onCancel()
            dismiss() }
        binding.confirm.setOnClickListener {

                onActionListener?.onConfirm()

            dismiss()
        }
    }


    override fun getLayoutId(): Int {
        return R.layout.dialog_bottom_update
    }

    override fun initViewBinding(v: View): DialogBottomUpdateBinding{
        return DialogBottomUpdateBinding.bind(v)
    }
}