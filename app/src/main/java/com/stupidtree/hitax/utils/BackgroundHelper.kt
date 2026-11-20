package com.stupidtree.hitax.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.SoftReference

object BackgroundHelper {
    @Volatile
    private var cachedBitmap: SoftReference<Bitmap>? = null

    @Volatile
    private var cacheKey: String = ""

    fun getProcessedBitmap(
        uri: String,
        blurRadius: Int,
        targetWidth: Int,
        targetHeight: Int,
        fitMode: String = "crop"
    ): Bitmap? {
        if (uri.isBlank()) return null
        if (targetWidth <= 0 || targetHeight <= 0) return null

        val key = "$uri|$blurRadius|$targetWidth|$targetHeight|$fitMode"
        cachedBitmap?.get()?.let {
            if (cacheKey == key && !it.isRecycled) return it
        }

        val file = File(uri.removePrefix("file://"))
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val scale = maxOf(
            1,
            minOf(
                options.outWidth / targetWidth,
                options.outHeight / targetHeight
            )
        )
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        val loaded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: return null

        val fitBitmap = when (fitMode) {
            "stretch" -> {
                // Original behavior: force to exact size
                val s = Bitmap.createScaledBitmap(loaded, targetWidth, targetHeight, true)
                if (loaded !== s) loaded.recycle()
                s
            }
            "fit" -> {
                // Fit inside: scale down to fit, keep aspect ratio, center
                val srcW = loaded.width.toFloat()
                val srcH = loaded.height.toFloat()
                val fitScale = minOf(targetWidth / srcW, targetHeight / srcH)
                val sw = (srcW * fitScale).toInt()
                val sh = (srcH * fitScale).toInt()
                val scaled = Bitmap.createScaledBitmap(loaded, sw, sh, true)
                if (loaded !== scaled) loaded.recycle()
                // Center on a transparent canvas of target size
                val canvas = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvasC = android.graphics.Canvas(canvas)
                canvasC.drawBitmap(scaled, ((targetWidth - sw) / 2).toFloat(), ((targetHeight - sh) / 2).toFloat(), null)
                scaled.recycle()
                canvas
            }
            else -> {
                // Default crop: fill and center-crop
                val srcW = loaded.width.toFloat()
                val srcH = loaded.height.toFloat()
                val cropScale = maxOf(targetWidth / srcW, targetHeight / srcH)
                val scaledW = (srcW * cropScale).toInt()
                val scaledH = (srcH * cropScale).toInt()
                var scaledBitmap: Bitmap = if (scaledW != loaded.width || scaledH != loaded.height) {
                    Bitmap.createScaledBitmap(loaded, scaledW, scaledH, true)
                } else { loaded }
                if (loaded !== scaledBitmap) loaded.recycle()
                val cropX = (scaledW - targetWidth) / 2
                val cropY = (scaledH - targetHeight) / 2
                val cropped = Bitmap.createBitmap(scaledBitmap, cropX, cropY, targetWidth, targetHeight)
                if (cropped !== scaledBitmap) scaledBitmap.recycle()
                cropped
            }
        }

        val result = if (blurRadius > 0) {
            applyFastBlur(fitBitmap, blurRadius)
        } else {
            fitBitmap
        }

        cachedBitmap = SoftReference(result)
        cacheKey = key
        return result
    }

    private fun applyFastBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val downScale = maxOf(1, radius / 3)
        val smallW = bitmap.width / downScale
        val smallH = bitmap.height / downScale
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)

        val result = boxBlur(small, maxOf(1, radius / downScale))
        if (small != result) small.recycle()
        return result
    }

    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source

        val w = source.width
        val h = source.height
        // Safety guard: skip blur if bitmap is too large for IntArray allocation
        if (w * h > 2_000_000) return source
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val result = IntArray(w * h)
        val r = radius.coerceIn(1, 254)

        // Horizontal pass
        for (y in 0 until h) {
            var sumA = 0L; var sumR = 0L; var sumG = 0L; var sumB = 0L
            var count = 0L
            for (x in -r..r) {
                val idx = y * w + x.coerceIn(0, w - 1)
                val c = pixels[idx]
                sumA += (c shr 24) and 0xFF
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
                count++
            }
            for (x in 0 until w) {
                result[y * w + x] = pack4(
                    (sumA / count).toInt(),
                    (sumR / count).toInt(),
                    (sumG / count).toInt(),
                    (sumB / count).toInt()
                )
                val left = y * w + (x - r).coerceIn(0, w - 1)
                val right = y * w + (x + r + 1).coerceIn(0, w - 1)
                val lc = pixels[left]
                val rc = pixels[right]
                sumA -= (lc shr 24) and 0xFF; sumA += (rc shr 24) and 0xFF
                sumR -= (lc shr 16) and 0xFF; sumR += (rc shr 16) and 0xFF
                sumG -= (lc shr 8) and 0xFF; sumG += (rc shr 8) and 0xFF
                sumB -= lc and 0xFF; sumB += rc and 0xFF
            }
        }

        val temp = result.copyOf()
        // Vertical pass
        for (x in 0 until w) {
            var sumA = 0L; var sumR = 0L; var sumG = 0L; var sumB = 0L
            var count = 0L
            for (y in -r..r) {
                val idx = (y.coerceIn(0, h - 1)) * w + x
                val c = temp[idx]
                sumA += (c shr 24) and 0xFF
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
                count++
            }
            for (y in 0 until h) {
                result[y * w + x] = pack4(
                    (sumA / count).toInt(),
                    (sumR / count).toInt(),
                    (sumG / count).toInt(),
                    (sumB / count).toInt()
                )
                val top = (y - r).coerceIn(0, h - 1) * w + x
                val bottom = (y + r + 1).coerceIn(0, h - 1) * w + x
                val tc = temp[top]
                val bc = temp[bottom]
                sumA -= (tc shr 24) and 0xFF; sumA += (bc shr 24) and 0xFF
                sumR -= (tc shr 16) and 0xFF; sumR += (bc shr 16) and 0xFF
                sumG -= (tc shr 8) and 0xFF; sumG += (bc shr 8) and 0xFF
                sumB -= tc and 0xFF; sumB += bc and 0xFF
            }
        }

        source.getPixels(pixels, 0, w, 0, 0, w, h) // reuse pixels array
        System.arraycopy(result, 0, pixels, 0, w * h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun pack4(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or
        (r.coerceIn(0, 255) shl 16) or
        (g.coerceIn(0, 255) shl 8) or
        b.coerceIn(0, 255)

    fun copyToInternalStorage(context: Context, uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: return ""
        val fileName = "background_${System.currentTimeMillis()}.jpg"
        val outputFile = File(context.filesDir, fileName)
        input.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return Uri.fromFile(outputFile).toString()
    }

    fun clearCache() {
        cachedBitmap?.get()?.recycle()
        cachedBitmap = null
        cacheKey = ""
    }
}
