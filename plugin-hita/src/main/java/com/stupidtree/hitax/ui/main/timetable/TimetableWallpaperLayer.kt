package com.stupidtree.hitax.ui.main.timetable

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Only the background is blurred; timetable labels and course cards stay sharp. */
internal fun attachTimetableWallpaper(root: ViewGroup, owner: LifecycleOwner) {
    val image = ImageView(root.context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    val scrim = View(root.context)
    val layer = FrameLayout(root.context).apply {
        isClickable = false
        addView(image, FrameLayout.LayoutParams(-1, -1))
        addView(scrim, FrameLayout.LayoutParams(-1, -1))
    }
    root.addView(layer, 0, ViewGroup.LayoutParams(-1, -1))
    val service = TimetableAppearanceStore.get(root.context)
    var job: Job? = null
    var key: Pair<Long, Int>? = null
    service.observe().observe(owner) { state ->
        scrim.setBackgroundColor(Color.argb(state.scrimOpacity * 255 / 100, 0, 0, 0))
        layer.visibility = if (state.hasWallpaper) View.VISIBLE else View.GONE
        if (Build.VERSION.SDK_INT >= 31) image.setRenderEffect(if (state.blurRadius > 0) RenderEffect.createBlurEffect(state.blurRadius.toFloat(), state.blurRadius.toFloat(), Shader.TileMode.CLAMP) else null)
        val next = state.revision to (if (Build.VERSION.SDK_INT >= 31) 0 else state.blurRadius)
        if (key != next) {
            key = next; job?.cancel()
            job = owner.lifecycleScope.launch {
                if (Build.VERSION.SDK_INT < 31) delay(120)
                val bitmap = withContext(Dispatchers.IO) {
                    val decoded = runCatching { service.openWallpaper()?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
                    if (Build.VERSION.SDK_INT < 31 && state.blurRadius > 0 && decoded != null) {
                        legacyBlur(decoded, state.blurRadius)
                    } else decoded
                }
                image.setImageBitmap(bitmap)
            }
        }
    }
}

/** Two separable box passes on a bounded thumbnail for Android 6–11. */
private fun legacyBlur(original: Bitmap, radius: Int): Bitmap {
    val scale = minOf(1f, 512f / maxOf(original.width, original.height))
    val bitmap = Bitmap.createScaledBitmap(original, maxOf(1, (original.width * scale).toInt()), maxOf(1, (original.height * scale).toInt()), true)
    val w = bitmap.width; val h = bitmap.height
    val pixels = IntArray(w * h); bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val out = IntArray(pixels.size)
    val r = maxOf(1, (radius * scale).toInt())
    repeat(2) {
        for (y in 0 until h) for (x in 0 until w) {
            var red = 0; var green = 0; var blue = 0
            for (dx in -r..r) { val c = pixels[y * w + (x + dx).coerceIn(0, w - 1)]; red += c shr 16 and 255; green += c shr 8 and 255; blue += c and 255 }
            val n = 2 * r + 1; out[y * w + x] = Color.rgb(red / n, green / n, blue / n)
        }
        for (y in 0 until h) for (x in 0 until w) {
            var red = 0; var green = 0; var blue = 0
            for (dy in -r..r) { val c = out[(y + dy).coerceIn(0, h - 1) * w + x]; red += c shr 16 and 255; green += c shr 8 and 255; blue += c and 255 }
            val n = 2 * r + 1; pixels[y * w + x] = Color.rgb(red / n, green / n, blue / n)
        }
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}
