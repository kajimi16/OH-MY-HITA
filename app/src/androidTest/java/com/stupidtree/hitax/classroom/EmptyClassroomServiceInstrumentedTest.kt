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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
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
        assumeTrue("已登录教务，跳过未登录断言", state?.state == DataState.STATE.NOT_LOGGED_IN)
        assertEquals(DataState.STATE.NOT_LOGGED_IN, state?.state)
    }

    @Test
    fun queryWithoutLoginReturnsNotLoggedIn() {
        val state = await(service().query("any", 1))
        assumeTrue("已登录教务，跳过未登录断言", state?.state == DataState.STATE.NOT_LOGGED_IN)
        assertEquals(DataState.STATE.NOT_LOGGED_IN, state?.state)
    }


    @Test
    fun realSessionReturnsBuildingsAndRoomsWhenLoggedIn() {
        val service = service()
        val buildings = await(service.buildings())
        assumeTrue("未登录教务，跳过真实数据验收", buildings?.state == DataState.STATE.SUCCESS)
        val building = buildings?.data?.firstOrNull()
        assertNotNull("教学楼列表为空", building)
        val rooms = await(service.query(building!!.id, 10))
        assertEquals(DataState.STATE.SUCCESS, rooms?.state)
        assertTrue("空教室列表为空", !rooms?.data.isNullOrEmpty())
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
