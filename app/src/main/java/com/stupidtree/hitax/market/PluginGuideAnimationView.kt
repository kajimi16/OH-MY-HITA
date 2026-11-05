package com.stupidtree.hitax.market

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat

/**
 * 插件发布流程的 MG 动画：编写 → 推送 → CI 构建 → 更新索引 → 市场安装。
 *
 * 纯 Canvas 绘制 + ValueAnimator 循环，不依赖视频或 Lottie。
 */
class PluginGuideAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class Step(val title: String, val subtitle: String)

    private val steps = listOf(
        Step("编写插件", "模板工程"),
        Step("推送仓库", "GitHub"),
        Step("CI 构建", "APK / dex"),
        Step("更新索引", "SHA-256"),
        Step("市场安装", "用户侧"),
    )

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 9000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private val accent = colorAttr(com.stupidtree.style.R.attr.colorPrimary)
    private val textColor = colorAttr(com.stupidtree.style.R.attr.textColorPrimary)
    private val muted = colorAttr(com.stupidtree.style.R.attr.textColorSecondary)
    private val cardColor = colorAttr(com.stupidtree.style.R.attr.backgroundColorSecond)

    init {
        textPaint.color = textColor
        subPaint.color = muted
        dotPaint.color = accent
        textPaint.textSize = sp(12f)
        subPaint.textSize = sp(9f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val pad = dp(12f)
        val gap = dp(10f)
        val cardWidth = (width - pad * 2 - gap * (steps.size - 1)) / steps.size
        val cardHeight = dp(92f)
        val top = (height - cardHeight) / 2f + dp(6f)
        val active = ((phase * steps.size).toInt()).coerceIn(0, steps.size - 1)

        // 连接线
        val lineY = top + cardHeight / 2f
        for (i in 0 until steps.size - 1) {
            val startX = pad + cardWidth * (i + 1) + gap * i
            val endX = pad + cardWidth * (i + 1) + gap * (i + 1)
            linePaint.color = if (i < active) accent else muted
            linePaint.alpha = if (i < active) 255 else 70
            linePaint.strokeWidth = dp(2f)
            canvas.drawLine(startX, lineY, endX, lineY, linePaint)

            if (i == active) {
                val local = (phase * steps.size - active).coerceIn(0f, 1f)
                val dotX = startX + (endX - startX) * local
                canvas.drawCircle(dotX, lineY, dp(4f), dotPaint)
                canvas.drawCircle(dotX, lineY, dp(9f), dotPaint.apply {
                    alpha = 60
                })
                dotPaint.alpha = 255
            }
        }

        steps.forEachIndexed { index, step ->
            val left = pad + (cardWidth + gap) * index
            val rect = RectF(left, top, left + cardWidth, top + cardHeight)
            val isActive = index == active
            fill.color = cardColor
            fill.alpha = if (isActive) 255 else 200
            canvas.drawRoundRect(rect, dp(14f), dp(14f), fill)

            stroke.color = if (isActive) accent else muted
            stroke.alpha = if (isActive) 255 else 60
            canvas.drawRoundRect(rect, dp(14f), dp(14f), stroke)

            if (isActive) {
                val pulse = (phase * steps.size - index).coerceIn(0f, 1f)
                stroke.color = accent
                stroke.alpha = (140 * (1f - pulse)).toInt().coerceIn(0, 255)
                stroke.strokeWidth = dp(4f)
                canvas.drawRoundRect(rect, dp(14f), dp(14f), stroke)
                stroke.strokeWidth = dp(1.5f)
            }

            val centerX = rect.centerX()
            val numberY = top + dp(30f)
            fill.color = if (isActive) accent else muted
            fill.alpha = if (isActive) 255 else 80
            canvas.drawCircle(centerX, numberY, dp(11f), fill)
            val numberPaint = subPaint.apply {
                color = if (isActive) Color.WHITE else textColor
                textSize = sp(10f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText(
                (index + 1).toString(),
                centerX,
                numberY - (numberPaint.descent() + numberPaint.ascent()) / 2f,
                numberPaint,
            )

            textPaint.color = if (isActive) accent else textColor
            textPaint.textSize = sp(12f)
            canvas.drawText(step.title, centerX, top + dp(60f), textPaint)
            subPaint.color = muted
            subPaint.textSize = sp(9f)
            canvas.drawText(step.subtitle, centerX, top + dp(77f), subPaint)
        }
    }

    private fun colorAttr(attr: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId)
            else value.data
        } else {
            Color.GRAY
        }
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
