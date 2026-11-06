package com.stupidtree.hitax.classroom

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stupidtree.hita.api.EmptyClassroomService
import com.stupidtree.hitax.HApplication
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyClassroomServiceInstrumentedTest {

    @Test
    fun serviceIsRegisteredByEasPlugin() {
        val app = ApplicationProvider.getApplicationContext<HApplication>()
        val service = app.pluginHost.service(EmptyClassroomService.KEY)
        assertNotNull("EmptyClassroomService 未注册", service)
    }
}
