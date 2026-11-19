package com.stupidtree.hitax.market

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import com.stupidtree.hitax.R

/**
 * 初始加载占位。只在市场没有可展示内容时显示，避免刷新时遮挡已有列表。
 *
 * 前置条件：无。
 * 后置条件：可见时启动一个往复透明度动画；不可见/离开窗口时停止。
 */
class MarketSkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val pulse = ValueAnimator.ofFloat(0.45f, 0.9f).apply {
        duration = 900L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { alpha = it.animatedValue as Float }
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_market_skeleton, this, true)
        alpha = 0.45f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        pulse.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (isAttachedToWindow) updateAnimation()
    }

    private fun updateAnimation() {
        if (visibility == VISIBLE && isAttachedToWindow) {
            if (!pulse.isStarted) pulse.start()
        } else {
            pulse.cancel()
            alpha = 0.45f
        }
    }
}
