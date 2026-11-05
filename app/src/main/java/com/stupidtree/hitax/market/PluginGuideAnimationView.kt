package com.stupidtree.hitax.market

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 插件发布流程 MG 动画：6 个步骤、流光连接线、粒子与完成态。
 *
 * 纯 Canvas + ValueAnimator，跟随主题色，不依赖视频/Lottie。
 */
class PluginGuideAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class Step(val title: String, val subtitle: String)

    private val steps = listOf(
        Step("编写插件", "ComposeUiPlugin / Plugin"),
        Step("推送仓库", "GitHub"),
        Step("发布请求", "Issue 模板"),
        Step("CI 构建", "校验 + sha256"),
        Step("更新索引", "registry"),
        Step("市场安装", "一键安装"),
    )

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val baseLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val accentLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgDot = Paint(Paint.ANTI_ALIAS_FLAG)

    private val accent = colorAttr(com.stupidtree.style.R.attr.colorPrimary)
    private val textColor = colorAttr(com.stupidtree.style.R.attr.textColorPrimary)
    private val muted = colorAttr(com.stupidtree.style.R.attr.textColorSecondary)
    private val cardColor = colorAttr(com.stupidtree.style.R.attr.backgroundColorSecond)

    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 12000
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private var cardRects: List<RectF> = emptyList()
    private var connectorPaths: List<Path> = emptyList()
    private val segmentPath = Path()
    private val tempRect = RectF()
    private val measure = PathMeasure()
    private val pos = FloatArray(2)
    private val tan = FloatArray(2)

    init {
        textPaint.color = textColor
        subPaint.color = muted
        baseLine.color = muted
        baseLine.strokeWidth = dp(1.5f)
        baseLine.pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(4f)), 0f)
        accentLine.color = accent
        accentLine.strokeWidth = dp(2f)
        iconPaint.color = accent
        iconPaint.strokeWidth = dp(1.8f)
        dotPaint.color = accent
        bgDot.color = muted
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildGeometry()
    }

    private fun rebuildGeometry() {
        if (width == 0 || height == 0) return
        val pad = dp(10f)
        val gap = dp(10f)
        val cardHeight = dp(112f)
        val rowGap = dp(34f)
        val cardWidth = (width - pad * 2 - gap * 2) / 3f
        val totalHeight = cardHeight * 2 + rowGap
        val top = (height - totalHeight) / 2f
        cardRects = listOf(
            RectF(pad, top, pad + cardWidth, top + cardHeight),
            RectF(pad + cardWidth + gap, top, pad + cardWidth * 2 + gap, top + cardHeight),
            RectF(pad + (cardWidth + gap) * 2, top, width - pad, top + cardHeight),
            RectF(pad, top + cardHeight + rowGap, pad + cardWidth, top + cardHeight * 2 + rowGap),
            RectF(pad + cardWidth + gap, top + cardHeight + rowGap, pad + cardWidth * 2 + gap, top + cardHeight * 2 + rowGap),
            RectF(pad + (cardWidth + gap) * 2, top + cardHeight + rowGap, width - pad, top + cardHeight * 2 + rowGap),
        )
        buildPaths(cardRects)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || cardRects.isEmpty()) return

        drawBackgroundDots(canvas)

        val rects = cardRects
        val progress = phase * steps.size
        val active = progress.toInt().coerceIn(0, steps.size - 1)

        // 连接线
        connectorPaths.forEachIndexed { index, path ->
            canvas.drawPath(path, baseLine)
            val local = (progress - index).coerceIn(0f, 1f)
            if (local > 0f) {
                measure.setPath(path, false)
                segmentPath.reset()
                measure.getSegment(0f, measure.length * local, segmentPath, true)
                canvas.drawPath(segmentPath, accentLine)
                if (index == active && local < 1f) {
                    measure.getPosTan(measure.length * local, pos, tan)
                    drawComet(canvas, pos[0], pos[1])
                }
            }
        }

        drawParticles(canvas, rects[active])

        rects.forEachIndexed { index, rect ->
            drawCard(canvas, rect, index, active, progress)
        }
    }

    private fun drawParticles(canvas: Canvas, rect: RectF) {
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        for (i in 0 until 12) {
            val angle = phase * Math.PI * 2 + i * Math.PI / 6
            val radius = dp(34f) + dp(8f) * sin((phase * 2 + i).toFloat())
            val x = centerX + cos(angle).toFloat() * radius * 1.45f
            val y = centerY + sin(angle).toFloat() * radius
            val shimmer = ((sin(angle + phase * 4) + 1.0) / 2.0).toFloat()
            dotPaint.color = accent
            dotPaint.alpha = (30 + shimmer * 90).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, dp(1.4f + shimmer * 1.2f), dotPaint)
        }
        dotPaint.alpha = 255
    }

    private fun buildPaths(rects: List<RectF>) {
        val paths = mutableListOf<Path>()
        // 0→1, 1→2 水平
        paths += Path().apply {
            moveTo(rects[0].right, rects[0].centerY())
            lineTo(rects[1].left, rects[1].centerY())
        }
        paths += Path().apply {
            moveTo(rects[1].right, rects[1].centerY())
            lineTo(rects[2].left, rects[2].centerY())
        }
        // 2→3 曲线折返到下一行左侧
        val midY = (rects[2].bottom + rects[3].top) / 2f
        paths += Path().apply {
            moveTo(rects[2].centerX(), rects[2].bottom)
            cubicTo(
                rects[2].centerX(), midY,
                rects[3].centerX(), midY,
                rects[3].centerX(), rects[3].top,
            )
        }
        paths += Path().apply {
            moveTo(rects[3].right, rects[3].centerY())
            lineTo(rects[4].left, rects[4].centerY())
        }
        paths += Path().apply {
            moveTo(rects[4].right, rects[4].centerY())
            lineTo(rects[5].left, rects[5].centerY())
        }
        connectorPaths = paths
    }

    private fun drawCard(
        canvas: Canvas,
        rect: RectF,
        index: Int,
        active: Int,
        progress: Float,
    ) {
        val completed = index < active
        val isActive = index == active
        val pulse = if (isActive) {
            val local = (progress - active).coerceIn(0f, 1f)
            (sin(local * Math.PI)).toFloat().coerceIn(0f, 1f)
        } else 0f

        val scale = 1f + pulse * 0.035f
        val pivotX = rect.centerX()
        val pivotY = rect.centerY()
        canvas.save()
        canvas.scale(scale, scale, pivotX, pivotY)

        if (isActive) {
            for (layer in 3 downTo 1) {
                glow.color = accent
                glow.alpha = (pulse * 18 * layer).toInt().coerceIn(0, 60)
                glow.strokeWidth = dp(2f + layer * 2f)
                tempRect.set(rect)
                tempRect.inset(-dp(layer * 1.2f), -dp(layer * 1.2f))
                canvas.drawRoundRect(tempRect, dp(16f), dp(16f), glow)
            }
        }

        fill.color = cardColor
        fill.alpha = if (isActive || completed) 255 else 190
        canvas.drawRoundRect(rect, dp(16f), dp(16f), fill)

        stroke.color = when {
            isActive -> accent
            completed -> accent
            else -> muted
        }
        stroke.alpha = when {
            isActive -> 255
            completed -> 110
            else -> 55
        }
        stroke.strokeWidth = dp(1.5f)
        canvas.drawRoundRect(rect, dp(16f), dp(16f), stroke)

        // 图标圆
        val iconCenterX = rect.centerX()
        val iconCenterY = rect.top + dp(34f)
        fill.color = when {
            isActive -> accent
            completed -> accent
            else -> muted
        }
        fill.alpha = when {
            isActive -> 255
            completed -> 40
            else -> 26
        }
        canvas.drawCircle(iconCenterX, iconCenterY, dp(17f), fill)
        iconPaint.color = when {
            isActive -> Color.WHITE
            completed -> accent
            else -> muted
        }
        iconPaint.alpha = if (isActive || completed) 255 else 130
        drawIcon(canvas, index, iconCenterX, iconCenterY)

        // 完成勾
        if (completed) {
            dotPaint.color = accent
            canvas.drawCircle(rect.right - dp(14f), rect.top + dp(14f), dp(7f), dotPaint)
            iconPaint.color = Color.WHITE
            iconPaint.strokeWidth = dp(1.8f)
            val cx = rect.right - dp(14f)
            val cy = rect.top + dp(14f)
            canvas.drawLine(cx - dp(3f), cy, cx - dp(1f), cy + dp(2.5f), iconPaint)
            canvas.drawLine(cx - dp(1f), cy + dp(2.5f), cx + dp(3.5f), cy - dp(3f), iconPaint)
        }

        val maxTextWidth = rect.width() - dp(14f)
        textPaint.color = if (isActive) accent else textColor
        textPaint.alpha = if (isActive || completed) 255 else 190
        drawFitted(canvas, steps[index].title, rect.centerX(), rect.top + dp(68f), textPaint, sp(12f), maxTextWidth)

        subPaint.color = muted
        subPaint.alpha = if (isActive) 230 else 160
        drawFitted(canvas, steps[index].subtitle, rect.centerX(), rect.top + dp(86f), subPaint, sp(8.5f), maxTextWidth)

        canvas.restore()
    }

    private fun drawFitted(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        paint: Paint,
        baseSize: Float,
        maxWidth: Float,
    ) {
        paint.textSize = baseSize
        val width = paint.measureText(text)
        if (width > maxWidth && width > 0f) {
            paint.textSize = (baseSize * maxWidth / width).coerceAtLeast(sp(6.5f))
        }
        canvas.drawText(text, centerX, baselineY, paint)
    }

    private fun drawComet(canvas: Canvas, x: Float, y: Float) {
        dotPaint.color = accent
        dotPaint.alpha = 40
        canvas.drawCircle(x, y, dp(10f), dotPaint)
        dotPaint.alpha = 90
        canvas.drawCircle(x, y, dp(6f), dotPaint)
        dotPaint.alpha = 255
        canvas.drawCircle(x, y, dp(3f), dotPaint)
    }

    private fun drawBackgroundDots(canvas: Canvas) {
        bgDot.alpha = 18
        val step = dp(28f)
        var y = step
        while (y < height) {
            var x = step
            while (x < width) {
                canvas.drawCircle(x, y, dp(1.1f), bgDot)
                x += step
            }
            y += step
        }
    }

    private fun drawIcon(canvas: Canvas, index: Int, cx: Float, cy: Float) {
        when (index) {
            0 -> { // </>
                canvas.drawLine(cx - dp(7f), cy - dp(3f), cx - dp(3f), cy + dp(2f), iconPaint)
                canvas.drawLine(cx - dp(3f), cy + dp(2f), cx - dp(7f), cy + dp(7f), iconPaint)
                canvas.drawLine(cx + dp(7f), cy - dp(3f), cx + dp(3f), cy + dp(2f), iconPaint)
                canvas.drawLine(cx + dp(3f), cy + dp(2f), cx + dp(7f), cy + dp(7f), iconPaint)
                canvas.drawLine(cx + dp(1f), cy - dp(7f), cx - dp(1f), cy + dp(8f), iconPaint)
            }
            1 -> { // git branch
                canvas.drawCircle(cx - dp(5f), cy + dp(6f), dp(2.5f), iconPaint)
                canvas.drawCircle(cx + dp(6f), cy - dp(5f), dp(2.5f), iconPaint)
                canvas.drawLine(cx - dp(5f), cy + dp(3.5f), cx - dp(5f), cy - dp(2f), iconPaint)
                canvas.drawLine(cx - dp(5f), cy - dp(2f), cx + dp(3f), cy - dp(2f), iconPaint)
                canvas.drawLine(cx + dp(3f), cy - dp(2f), cx + dp(3f), cy - dp(5f), iconPaint)
            }
            2 -> { // issue
                val r = RectF(cx - dp(9f), cy - dp(7f), cx + dp(9f), cy + dp(7f))
                canvas.drawRoundRect(r, dp(4f), dp(4f), iconPaint)
                canvas.drawLine(cx - dp(5f), cy - dp(2f), cx + dp(5f), cy - dp(2f), iconPaint)
                canvas.drawLine(cx - dp(5f), cy + dp(2f), cx + dp(2f), cy + dp(2f), iconPaint)
            }
            3 -> { // build gear/check
                canvas.drawCircle(cx, cy, dp(7f), iconPaint)
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    canvas.drawLine(
                        cx + cos(a).toFloat() * dp(9f), cy + sin(a).toFloat() * dp(9f),
                        cx + cos(a).toFloat() * dp(12f), cy + sin(a).toFloat() * dp(12f),
                        iconPaint,
                    )
                }
            }
            4 -> { // database
                for (i in 0 until 3) {
                    val y = cy - dp(6f) + i * dp(5f)
                    val r = RectF(cx - dp(10f), y - dp(3f), cx + dp(10f), y + dp(3f))
                    canvas.drawOval(r, iconPaint)
                }
            }
            else -> { // download
                canvas.drawLine(cx, cy - dp(8f), cx, cy + dp(4f), iconPaint)
                canvas.drawLine(cx - dp(5f), cy - dp(1f), cx, cy + dp(4f), iconPaint)
                canvas.drawLine(cx + dp(5f), cy - dp(1f), cx, cy + dp(4f), iconPaint)
                canvas.drawLine(cx - dp(8f), cy + dp(8f), cx + dp(8f), cy + dp(8f), iconPaint)
            }
        }
    }

    private fun colorAttr(attr: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId)
            else value.data
        } else Color.GRAY
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
