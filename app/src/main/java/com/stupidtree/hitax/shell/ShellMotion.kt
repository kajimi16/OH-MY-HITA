package com.stupidtree.hitax.shell

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Pure motion math shared by the Compose shell and JVM tests.
 *
 * Source: HITA_Android v3.1.2 (commit ac15007), DockMetrics.kt / MainScreen.kt.
 * No Android or Compose types are allowed here.
 */
internal object ShellMotion {

    // Drawer (MainScreen.kt v3.1.2)
    const val DRAWER_GLASS_OPEN_MS = 240
    const val DRAWER_GLASS_CLOSE_MS = 200
    const val DRAWER_GLASS_SCALE_DELTA = 0.008f
    const val DRAWER_NORMAL_SCALE_DELTA = 0.2f
    const val DRAWER_GLASS_OFFSET_DP = 10f

    // Dock (DockMetrics.kt v3.1.2)
    const val DOCK_BAR_PADDING = 4f
    const val DOCK_OVERDRAG = 0.35f
    const val DOCK_JELLY_SPEED = 8f
    const val DOCK_JELLY_STRETCH = 0.25f
    const val DOCK_FLING_PROJECTION_SECONDS = 0.1f
    const val DOCK_PRESS_GROWTH = 1f / 8f

    fun drawerScale(style: String, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (style == "apple_glass") {
            1f - p * DRAWER_GLASS_SCALE_DELTA
        } else {
            1f - p * DRAWER_NORMAL_SCALE_DELTA
        }
    }

    fun drawerOffsetPx(style: String, progress: Float, drawerWidthPx: Float, density: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        if (drawerWidthPx <= 0f) return 0f
        return if (style == "apple_glass") {
            -DRAWER_GLASS_OFFSET_DP * density * p
        } else {
            -drawerWidthPx * p
        }
    }

    fun drawerDurationMs(style: String, opening: Boolean): Int =
        if (style != "apple_glass") 0 else if (opening) DRAWER_GLASS_OPEN_MS else DRAWER_GLASS_CLOSE_MS

    /** Asymptotic iOS-style rubber band: output is always strictly inside +/- limit. */
    fun rubberBand(overshoot: Float, limit: Float): Float {
        if (overshoot == 0f || limit <= 0f) return 0f
        val pull = 1f - 1f / (abs(overshoot) * 0.55f / limit + 1f)
        return limit * pull * sign(overshoot)
    }

    /** Local x -> floating dock index, with rubber-band outside [0, lastIndex]. */
    fun dockPositionAt(
        dx: Float,
        barWidth: Float,
        count: Int,
        padding: Float,
        overdrag: Float = DOCK_OVERDRAG,
    ): Float {
        if (count <= 0) return 0f
        val extent = (barWidth - padding * 2f) / count
        if (extent <= 0f) return 0f
        val last = (count - 1).toFloat()
        val position = (dx - padding) / extent - 0.5f
        return when {
            position < 0f -> rubberBand(position, overdrag)
            position > last -> last + rubberBand(position - last, overdrag)
            else -> position
        }
    }

    fun dockIndexAt(dx: Float, barWidth: Float, count: Int, padding: Float): Int {
        if (count <= 0) return 0
        return dockPositionAt(dx, barWidth, count, padding).roundToInt().coerceIn(0, count - 1)
    }

    /** Fling landing index, bounded to one item past the current pressed item. */
    fun flingIndex(
        lensTarget: Float,
        velocityX: Float,
        extent: Float,
        pressedIndex: Int,
        lastIndex: Int,
        projectionSeconds: Float = DOCK_FLING_PROJECTION_SECONDS,
    ): Int {
        if (extent <= 0f || lastIndex < 0) return pressedIndex.coerceIn(0, maxOf(lastIndex, 0))
        val lead = velocityX / extent * projectionSeconds
        return (lensTarget + lead)
            .roundToInt()
            .coerceIn(pressedIndex - 1, pressedIndex + 1)
            .coerceIn(0, lastIndex)
    }

    fun pressScale(swell: Float, maxDimensionPx: Float, maxGrowthPx: Float): Float {
        if (maxDimensionPx <= 0f) return 1f
        return 1f + swell.coerceIn(0f, 1f) * min(DOCK_PRESS_GROWTH * 2f, maxGrowthPx / maxDimensionPx)
    }
}
