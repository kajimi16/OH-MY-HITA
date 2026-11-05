package com.stupidtree.hitax.appearance

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stupidtree.hita.api.TimetableAppearanceService
import com.stupidtree.hita.api.TimetableAppearance
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimetableAppearanceStoreTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val token = "appearance-test-${UUID.randomUUID()}"
    private val directory = File(app.cacheDir, token).apply { mkdirs() }
    private val isolated = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = directory
        override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$token-$name", mode)
    }
    private fun newStore() = Class.forName("com.stupidtree.hitax.ui.main.timetable.TimetableAppearanceStore")
        .getConstructor(Context::class.java).newInstance(isolated) as TimetableAppearanceService
    private val store = newStore()
    private fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    private fun image(): ByteArray = ByteArrayOutputStream().also { output ->
        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(android.graphics.Color.BLUE); it.compress(Bitmap.CompressFormat.PNG, 100, output); it.recycle()
        }
    }.toByteArray()
    private fun bytes() = store.openWallpaper()?.use { it.readBytes() }

    @After fun cleanup() {
        store.reset(); idle()
        isolated.getSharedPreferences("timetable_style", 0).edit().clear().commit()
        directory.deleteRecursively()
    }

    @Test fun failedOrOversizedImportPreservesTheExistingImage() {
        ByteArrayInputStream(image()).use(store::importWallpaper); idle()
        val original = bytes()
        try { ByteArrayInputStream(byteArrayOf(1, 2, 3)).use(store::importWallpaper); fail("Invalid image accepted") }
        catch (_: IllegalArgumentException) {}
        assertArrayEquals(original, bytes())
        val endless = object : InputStream() {
            override fun read(): Int = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int { buffer.fill(0, offset, offset + length); return length }
        }
        try { endless.use(store::importWallpaper); fail("Oversized image accepted") }
        catch (_: IllegalArgumentException) {}
        assertArrayEquals(original, bytes())
    }

    @Test fun settingsCannotForgeWallpaperStateAndSurviveReopening() {
        store.update(TimetableAppearance(-20, Int.MAX_VALUE, 99, true, Long.MAX_VALUE)); idle()
        val value = store.observe().value!!
        assertEquals(TimetableAppearance(0, 30, 80, false, 0), value)
        assertEquals(value, newStore().observe().value)
    }

    @Test fun resettingRemovesImageAndRestoresDefaults() {
        ByteArrayInputStream(image()).use(store::importWallpaper); idle()
        assertTrue(store.observe().value!!.hasWallpaper)
        store.update(TimetableAppearance(25, 30, 50)); store.reset(); idle()
        assertNull(bytes())
        val value = store.observe().value!!
        assertFalse(value.hasWallpaper)
        assertEquals(95, value.cardOpacity)
        assertEquals(0, value.blurRadius)
        assertEquals(20, value.scrimOpacity)
    }
}
