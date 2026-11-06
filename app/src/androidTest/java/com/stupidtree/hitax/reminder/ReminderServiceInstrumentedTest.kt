package com.stupidtree.hitax.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stupidtree.hita.api.ReminderService
import com.stupidtree.hita.api.ReminderSpec
import com.stupidtree.hitax.HApplication
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderServiceInstrumentedTest {

    @Test
    fun scheduledReminderIsDeliveredAsNotification() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )

        val app = context.applicationContext as HApplication
        val service = requireNotNull(app.pluginHost.service(ReminderService.KEY))
        assertTrue(service.canPostNotifications())

        val id = "instrumented-${System.currentTimeMillis()}"
        val scheduled = service.schedule(
            ReminderSpec(
                ownerId = "com.stupidtree.hitax.test",
                id = id,
                triggerAtMillis = System.currentTimeMillis() + 2_000,
                title = "测试提醒",
                text = "提醒服务已送达",
            )
        )
        assertTrue("schedule returned false", scheduled)

        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + 15_000
        var delivered = false
        while (System.currentTimeMillis() < deadline) {
            delivered = manager.activeNotifications.any { sbn ->
                sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() == "测试提醒"
            }
            if (delivered) break
            Thread.sleep(500)
        }
        service.cancelAll("com.stupidtree.hitax.test")
        assertTrue("notification not delivered before deadline", delivered)
    }
}
