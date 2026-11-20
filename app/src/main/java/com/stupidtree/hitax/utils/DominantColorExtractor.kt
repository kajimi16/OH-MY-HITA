package com.stupidtree.hitax.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object DominantColorExtractor {
    private const val HUE_BUCKETS = 12
    private const val SAT_BUCKETS = 3
    private const val LIGHT_BUCKETS = 2
    private const val TOTAL_BINS = HUE_BUCKETS * SAT_BUCKETS * LIGHT_BUCKETS
    private const val SAMPLE_SIZE = 100

    fun extract(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        val scale = max(1, max(w, h) / SAMPLE_SIZE)
        val scaledW = w / scale
        val scaledH = h / scale

        // Histogram bins: count, sumR, sumG, sumB per bin
        val counts = IntArray(TOTAL_BINS)
        val sumsR = LongArray(TOTAL_BINS)
        val sumsG = LongArray(TOTAL_BINS)
        val sumsB = LongArray(TOTAL_BINS)

        for (y in 0 until scaledH) {
            for (x in 0 until scaledW) {
                val px = x * scale
                val py = y * scale
                val pixel = bitmap.getPixel(px, py)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val hsl = rgbToHsl(r, g, b)
                val hIdx = min((hsl[0] * HUE_BUCKETS).toInt(), HUE_BUCKETS - 1)
                val sIdx = if (hsl[1] < 0.02f) 0 else min((hsl[1] * SAT_BUCKETS).toInt(), SAT_BUCKETS - 1)
                val lIdx = if (hsl[2] < 0.15f) 0 else if (hsl[2] > 0.85f) LIGHT_BUCKETS - 1 else min((hsl[2] * LIGHT_BUCKETS).toInt(), LIGHT_BUCKETS - 1)
                val bin = (hIdx * SAT_BUCKETS + sIdx) * LIGHT_BUCKETS + lIdx

                counts[bin]++
                sumsR[bin] += r.toLong()
                sumsG[bin] += g.toLong()
                sumsB[bin] += b.toLong()
            }
        }

        // Find dominant bin (most populated, preferring saturated bins)
        var bestBin = 0
        var bestScore = -1L
        for (i in 0 until TOTAL_BINS) {
            if (counts[i] == 0) continue
            val sLevel = (i / LIGHT_BUCKETS) % SAT_BUCKETS
            val score = counts[i].toLong() * (1 + sLevel) // favor saturated colors
            if (score > bestScore) {
                bestScore = score
                bestBin = i
            }
        }

        if (counts[bestBin] == 0) return Color.parseColor("#304ffe")

        val avgR = (sumsR[bestBin] / counts[bestBin]).toInt()
        val avgG = (sumsG[bestBin] / counts[bestBin]).toInt()
        val avgB = (sumsB[bestBin] / counts[bestBin]).toInt()

        // Mild saturation boost for visual appeal
        val hsl = rgbToHsl(avgR, avgG, avgB)
        val boostedSat = (hsl[1] * 1.15f).coerceIn(0f, 1f)
        return hslToRgb(hsl[0], boostedSat, hsl[2])
    }

    data class BinInfo(val hue: Float, val sat: Float, val light: Float, val avgR: Int, val avgG: Int, val avgB: Int, val count: Int)

    fun extractPalette(bitmap: Bitmap): List<Int> {
        val w = bitmap.width
        val h = bitmap.height
        val scale = max(1, max(w, h) / 80)
        val scaledW = w / scale
        val scaledH = h / scale

        val counts = IntArray(TOTAL_BINS)
        val sumsR = LongArray(TOTAL_BINS)
        val sumsG = LongArray(TOTAL_BINS)
        val sumsB = LongArray(TOTAL_BINS)
        // Track average hue per bin
        val sumH = FloatArray(TOTAL_BINS)

        for (y in 0 until scaledH) {
            for (x in 0 until scaledW) {
                val px = x * scale
                val py = y * scale
                val pixel = bitmap.getPixel(px, py)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val hsl = rgbToHsl(r.toInt(), g.toInt(), b.toInt())
                // Skip near-white, near-black, near-gray
                if (hsl[2] > 0.92f || hsl[2] < 0.08f || hsl[1] < 0.06f) continue

                val hIdx = min((hsl[0] * HUE_BUCKETS).toInt(), HUE_BUCKETS - 1)
                val sIdx = min((hsl[1] * SAT_BUCKETS).toInt(), SAT_BUCKETS - 1)
                val lIdx = if (hsl[2] < 0.2f) 0 else if (hsl[2] > 0.8f) LIGHT_BUCKETS - 1 else 1
                val bin = (hIdx * SAT_BUCKETS + sIdx) * LIGHT_BUCKETS + lIdx

                counts[bin]++
                sumsR[bin] += r.toLong()
                sumsG[bin] += g.toLong()
                sumsB[bin] += b.toLong()
                sumH[bin] += hsl[0]
            }
        }

        // Build bin info list, filter invalid
        val bins = mutableListOf<BinInfo>()
        for (i in 0 until TOTAL_BINS) {
            if (counts[i] < 5) continue
            val avgH = sumH[i] / counts[i]
            val avgS = ((i / LIGHT_BUCKETS) % SAT_BUCKETS).toFloat() / SAT_BUCKETS + 0.15f
            val avgL = ((i % LIGHT_BUCKETS).toFloat() / LIGHT_BUCKETS + 0.15f).coerceIn(0f, 1f)
            bins.add(BinInfo(
                hue = avgH, sat = avgS, light = avgL,
                avgR = (sumsR[i] / counts[i]).toInt(),
                avgG = (sumsG[i] / counts[i]).toInt(),
                avgB = (sumsB[i] / counts[i]).toInt(),
                count = counts[i]
            ))
        }

        // Sort by population
        bins.sortByDescending { it.count }

        // Greedy selection: pick top bins ensuring hue diversity (>25° apart)
        val selected = mutableListOf<BinInfo>()
        for (bin in bins) {
            if (selected.size >= 4) break
            val minHueDist = selected.minOfOrNull { hueDist(bin.hue, it.hue) } ?: 1f
            if (minHueDist > 0.07f) { // ~25° in normalized hue
                selected.add(bin)
            }
        }

        // Fill remaining slots with varied-lightness variants of selected colors
        while (selected.size < 4 && bins.isNotEmpty()) {
            val fallback = bins.getOrElse(selected.size) { bins.first() }
            selected.add(fallback.copy(
                light = (fallback.light + (selected.size * 0.15f)).coerceIn(0.15f, 0.85f)
            ))
        }

        return selected.map { bin ->
            val hsl = rgbToHsl(bin.avgR, bin.avgG, bin.avgB)
            val boostedSat = (hsl[1] * 1.1f).coerceIn(0f, 1f)
            hslToRgb(hsl[0], boostedSat, bin.light.coerceIn(0.15f, 0.85f))
        }
    }

    private fun hueDist(h1: Float, h2: Float): Float {
        val d = kotlin.math.abs(h1 - h2)
        return if (d > 0.5f) 1f - d else d
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val mx = maxOf(rf, gf, bf)
        val mn = minOf(rf, gf, bf)
        val l = (mx + mn) / 2f
        if (mx == mn) return floatArrayOf(0f, 0f, l)
        val d = mx - mn
        val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            rf -> ((gf - bf) / d + if (gf < bf) 6f else 0f) / 6f
            gf -> ((bf - rf) / d + 2f) / 6f
            else -> ((rf - gf) / d + 4f) / 6f
        }
        return floatArrayOf(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Int {
        if (s == 0f) {
            val v = (l * 255).toInt().coerceIn(0, 255)
            return Color.rgb(v, v, v)
        }
        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        val r = (hue2rgb(p, q, h + 1f / 3f) * 255).toInt().coerceIn(0, 255)
        val g = (hue2rgb(p, q, h) * 255).toInt().coerceIn(0, 255)
        val b = (hue2rgb(p, q, h - 1f / 3f) * 255).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun hue2rgb(p: Float, q: Float, t: Float): Float {
        var tx = t
        if (tx < 0) tx += 1f
        if (tx > 1) tx -= 1f
        if (tx < 1f / 6f) return p + (q - p) * 6f * tx
        if (tx < 1f / 2f) return q
        if (tx < 2f / 3f) return p + (q - p) * (2f / 3f - tx) * 6f
        return p
    }
}
