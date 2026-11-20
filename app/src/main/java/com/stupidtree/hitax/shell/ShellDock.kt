package com.stupidtree.hitax.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Dock four-spring state machine, mirrored from HITA_Android v3.1.2. */
class ShellDockState internal constructor(
    private val scope: CoroutineScope,
    selected: Int,
    private val onSelect: (Int) -> Unit,
    private val onTick: () -> Unit,
) {
    val lens = Animatable(selected.toFloat())
    val lift = Animatable(0f)
    val swell = Animatable(0f)
    val stretch = Animatable(0f)

    private var pressedIndex = -1
    private var pressX = 0f
    private var dragging = false

    private val trackSpring = spring<Float>(dampingRatio = 1f, stiffness = 2740f)
    private val liftSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 504f)
    private val settleSpring = spring<Float>(dampingRatio = 0.68f, stiffness = 158f)

    fun press(x: Float, barWidth: Float, count: Int, padding: Float) {
        if (count <= 0) return
        val index = ShellMotion.dockIndexAt(x, barWidth, count, padding)
        pressedIndex = index
        pressX = x
        dragging = false
        scope.launch { lift.animateTo(1f, liftSpring) }
        scope.launch { swell.animateTo(1f, liftSpring) }
        scope.launch { lens.animateTo(index.toFloat(), settleSpring) }
    }

    fun drag(x: Float, barWidth: Float, count: Int, padding: Float, touchSlop: Float, shortSide: Float) {
        if (pressedIndex < 0) return
        if (!dragging) {
            if (abs(x - pressX) < touchSlop) return
            dragging = true
        }
        val extent = (barWidth - padding * 2f) / count.coerceAtLeast(1)
        if (extent > 0f) {
            scope.launch {
                stretch.animateTo(
                    ShellMotion.rubberBand(x - x.coerceIn(0f, barWidth), shortSide * ShellMotion.DOCK_PULL_LIMIT),
                    trackSpring,
                )
            }
            scope.launch {
                lens.animateTo(
                    ShellMotion.dockPositionAt(x, barWidth, count, padding),
                    trackSpring,
                )
            }
        }
        val index = ShellMotion.dockIndexAt(x, barWidth, count, padding)
        if (index != pressedIndex) {
            pressedIndex = index
            onTick()
        }
    }

    fun release(velocityX: Float, barWidth: Float, count: Int, padding: Float, selected: Int) {
        val index = pressedIndex
        val wasDragging = dragging
        pressedIndex = -1
        dragging = false
        scope.launch { lift.animateTo(0f, settleSpring) }
        scope.launch { swell.animateTo(0f, settleSpring) }
        scope.launch { stretch.animateTo(0f, settleSpring) }
        if (index < 0) {
            scope.launch { lens.animateTo(selected.toFloat(), settleSpring) }
            return
        }
        val extent = (barWidth - padding * 2f) / count.coerceAtLeast(1)
        val target = if (wasDragging) {
            ShellMotion.flingIndex(
                lensTarget = lens.value,
                velocityX = velocityX,
                extent = extent,
                pressedIndex = index,
                lastIndex = count - 1,
            )
        } else {
            index
        }
        scope.launch { lens.animateTo(target.toFloat(), settleSpring) }
        if (target != selected) {
            onTick()
            onSelect(target)
        }
    }

    fun sync(selected: Int) {
        if (pressedIndex < 0) {
            scope.launch { lens.animateTo(selected.toFloat(), settleSpring) }
        }
    }
}

@Composable
fun rememberShellDockState(
    selected: Int,
    onSelect: (Int) -> Unit,
    onTick: () -> Unit,
): ShellDockState {
    val scope = rememberCoroutineScope()
    val latestSelect = rememberUpdatedState(onSelect)
    val latestTick = rememberUpdatedState(onTick)
    val state = remember {
        ShellDockState(scope, selected, { latestSelect.value(it) }, { latestTick.value() })
    }
    LaunchedEffect(selected) { state.sync(selected) }
    return state
}
