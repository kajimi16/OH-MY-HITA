package com.stupidtree.hitax

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stupidtree.hitax.utils.ShareUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareUtilsTest {
    @Test
    fun buildShareIntentForUri_setsMimeStreamAndFlags() {
        val stream = Uri.parse("content://com.stupidtree.hitax.test/calendar.ics")
        val intent = ShareUtils.buildShareIntentForUri(stream, "text/calendar")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/calendar", intent.type)
        assertEquals(stream, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

}
