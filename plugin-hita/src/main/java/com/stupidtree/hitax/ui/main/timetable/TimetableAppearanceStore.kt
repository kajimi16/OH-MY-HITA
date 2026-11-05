package com.stupidtree.hitax.ui.main.timetable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.api.TimetableAppearance
import com.stupidtree.hita.api.TimetableAppearanceService
import java.io.File
import java.io.InputStream

class TimetableAppearanceStore internal constructor(context: Context) : TimetableAppearanceService {
    private val prefs = context.applicationContext.getSharedPreferences("timetable_style", Context.MODE_PRIVATE)
    private val file = AtomicFile(File(context.applicationContext.filesDir, "timetable-wallpaper.png"))
    private val main = Handler(Looper.getMainLooper())
    private fun snapshot() = TimetableAppearance(prefs.getInt("appearance_opacity", 95),
        prefs.getInt("appearance_blur", 0), prefs.getInt("appearance_scrim", 20), file.baseFile.isFile,
        prefs.getLong("appearance_revision", 0)).normalized()
    private val state = MutableLiveData(snapshot())
    override fun observe() = state
    private fun publish() {
        val next = snapshot()
        if (Looper.myLooper() == Looper.getMainLooper()) state.value = next else main.post { state.value = snapshot() }
    }
    @Synchronized override fun update(settings: TimetableAppearance) {
        val value = settings.normalized()
        prefs.edit().putInt("appearance_opacity", value.cardOpacity).putInt("appearance_blur", value.blurRadius)
            .putInt("appearance_scrim", value.scrimOpacity).apply()
        publish()
    }
    @Synchronized override fun importWallpaper(input: InputStream) {
        val output = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val count = input.read(chunk); if (count < 0) break
            require(output.size() + count <= 20 * 1024 * 1024) { "图片不能超过 20 MB" }
            output.write(chunk, 0, count)
        }
        val bytes = output.toByteArray()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("无法解码图片")
        val stream = file.startWrite()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            file.finishWrite(stream)
        } catch (error: Throwable) { file.failWrite(stream); throw error }
        finally { bitmap.recycle() }
        prefs.edit().putLong("appearance_revision", prefs.getLong("appearance_revision", 0) + 1).apply()
        publish()
    }
    @Synchronized override fun openWallpaper(): InputStream? = if (file.baseFile.isFile) file.openRead() else null
    @Synchronized override fun clearWallpaper() {
        file.delete()
        prefs.edit().putLong("appearance_revision", prefs.getLong("appearance_revision", 0) + 1).apply()
        publish()
    }
    override fun reset() { clearWallpaper(); update(TimetableAppearance()) }
    companion object {
        @Volatile private var instance: TimetableAppearanceStore? = null
        fun get(context: Context): TimetableAppearanceStore = instance ?: synchronized(this) {
            instance ?: TimetableAppearanceStore(context).also { instance = it }
        }
    }
}
