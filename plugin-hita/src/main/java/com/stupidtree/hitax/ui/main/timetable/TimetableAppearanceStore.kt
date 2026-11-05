package com.stupidtree.hitax.ui.main.timetable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.api.TimetableAppearance
import com.stupidtree.hita.api.TimetableAppearanceService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class TimetableAppearanceStore internal constructor(context: Context) : TimetableAppearanceService {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("timetable_style", Context.MODE_PRIVATE)
    private val file = AtomicFile(File(appContext.filesDir, "timetable-wallpaper.png"))
    private val main = Handler(Looper.getMainLooper())
    private var epoch = 0L

    private fun snapshot() = TimetableAppearance(
        prefs.getInt("appearance_opacity", 95),
        prefs.getInt("appearance_blur", 0),
        prefs.getInt("appearance_scrim", 20),
        file.baseFile.isFile,
        prefs.getLong("appearance_revision", 0),
    ).normalized()

    private val state = MutableLiveData(snapshot())

    override fun observe() = state

    private fun publish() {
        val next = snapshot()
        if (Looper.myLooper() == Looper.getMainLooper()) state.value = next
        else main.post { state.value = snapshot() }
    }

    @Synchronized
    override fun update(settings: TimetableAppearance) {
        val value = settings.normalized()
        prefs.edit()
            .putInt("appearance_opacity", value.cardOpacity)
            .putInt("appearance_blur", value.blurRadius)
            .putInt("appearance_scrim", value.scrimOpacity)
            .apply()
        publish()
    }

    override fun importWallpaper(input: InputStream) {
        // 读取/解码/旋转/压缩都在锁外完成，避免滑杆 update() 被大图处理阻塞。
        val token = synchronized(this) { epoch }
        val png = decodeAndEncode(input)
        synchronized(this) {
            check(token == epoch) { "外观已重置，取消导入" }
            writeWallpaper(png)
        }
    }

    private fun decodeAndEncode(input: InputStream): ByteArray {
        val source = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val count = input.read(chunk)
            if (count < 0) break
            require(source.size() + count <= 20 * 1024 * 1024) { "图片不能超过 20 MB" }
            source.write(chunk, 0, count)
        }
        val bytes = source.toByteArray()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        var bitmap = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: error("无法解码图片")
        bitmap = applyExifOrientation(bytes, bitmap)
        val output = ByteArrayOutputStream()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "图片编码失败" }
        } finally {
            bitmap.recycle()
        }
        return output.toByteArray()
    }

    private fun applyExifOrientation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_NORMAL) return bitmap
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    @Synchronized
    private fun writeWallpaper(png: ByteArray) {
        val stream = file.startWrite()
        try {
            stream.write(png)
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
        prefs.edit().putLong("appearance_revision", prefs.getLong("appearance_revision", 0) + 1).apply()
        publish()
    }

    @Synchronized
    override fun openWallpaper(): InputStream? = if (file.baseFile.isFile) file.openRead() else null

    @Synchronized
    override fun clearWallpaper() {
        epoch++
        file.delete()
        prefs.edit().putLong("appearance_revision", prefs.getLong("appearance_revision", 0) + 1).apply()
        publish()
    }

    @Synchronized
    override fun reset() {
        epoch++
        file.delete()
        prefs.edit()
            .putInt("appearance_opacity", 95)
            .putInt("appearance_blur", 0)
            .putInt("appearance_scrim", 20)
            .putLong("appearance_revision", prefs.getLong("appearance_revision", 0) + 1)
            .apply()
        publish()
    }

    companion object {
        @Volatile
        private var instance: TimetableAppearanceStore? = null

        fun get(context: Context): TimetableAppearanceStore = instance ?: synchronized(this) {
            instance ?: TimetableAppearanceStore(context).also { instance = it }
        }
    }
}
