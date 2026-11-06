package com.stupidtree.hitax.classroom

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.EmptyClassroomService
import com.stupidtree.hitax.HApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmptyClassroomServiceInstrumentedTest {

    private fun service(): EmptyClassroomService {
        val app = ApplicationProvider.getApplicationContext<HApplication>()
        return app.pluginHost.service(EmptyClassroomService.KEY)
            ?: error("EmptyClassroomService 未注册")
    }

    @Test
    fun serviceIsRegisteredByEasPlugin() {
        assertNotNull(service())
    }

    @Test
    fun buildingsWithoutLoginReturnsNotLoggedIn() {
        val state = await(service().buildings())
        assertNotNull(state)
        assertEquals(DataState.STATE.NOT_LOGGED_IN, state?.state)
    }

    @Test
    fun queryWithoutLoginReturnsNotLoggedIn() {
        val state = await(service().query("any", 1))
        assertNotNull(state)
        assertEquals(DataState.STATE.NOT_LOGGED_IN, state?.state)
    }

    private fun <T> await(liveData: LiveData<T>): T? {
        val latch = CountDownLatch(1)
        var value: T? = null
        val observer = object : Observer<T> {
            override fun onChanged(result: T) {
                value = result
                liveData.removeObserver(this)
                latch.countDown()
            }
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            liveData.observeForever(observer)
        }
        latch.await(8, TimeUnit.SECONDS)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            liveData.removeObserver(observer)
        }
        return value
    }
}
