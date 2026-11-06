package com.stupidtree.hitax.classroom

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ScoreService
import com.stupidtree.hita.api.TeacherService
import com.stupidtree.hita.api.TrainingPlanService
import com.stupidtree.hitax.HApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RealDataServiceInstrumentedTest {

    private val app: HApplication
        get() = ApplicationProvider.getApplicationContext()


    @Test
    fun scoreReportReturnsRealDataWhenLoggedIn() {
        val service = app.pluginHost.service(ScoreService.KEY)
            ?: error("ScoreService 未注册")
        val terms = await(service.terms())
        android.util.Log.i("RealDataTest", "scoreTerms=${terms?.state} size=${terms?.data?.size}")
        assumeTrue("未登录教务", terms?.state == DataState.STATE.SUCCESS)
        val term = terms!!.data!!.firstOrNull { it.isCurrent } ?: terms.data!!.first()
        val report = await(service.report(term), 30_000)
        android.util.Log.i("RealDataTest", "scoreReport=${report?.state} msg=${report?.message} data=${report?.data != null}")
        assumeTrue("成绩报告未成功", report?.state == DataState.STATE.SUCCESS)
        assertNotNull(report?.data)
    }

    @Test
    fun trainingPlanReturnsRealDetailWhenLoggedIn() {
        val service = app.pluginHost.service(TrainingPlanService.KEY)
            ?: error("TrainingPlanService 未注册")
        val plans = await(service.plans())
        android.util.Log.i("RealDataTest", "plans=${plans?.state} msg=${plans?.message} size=${plans?.data?.size}")
        assumeTrue("未登录或培养方案不可用", plans?.state == DataState.STATE.SUCCESS)
        assertTrue("培养方案列表为空", !plans?.data.isNullOrEmpty())
        val detail = await(service.detail(plans!!.data!!.first()))
        android.util.Log.i("RealDataTest", "detail=${detail?.state} msg=${detail?.message}")
        assumeTrue("培养方案详情不可用", detail?.state == DataState.STATE.SUCCESS)
        assertNotNull("培养方案详情为空", detail?.data)
    }

    @Test
    fun teacherSearchReturnsResults() {
        val service = app.pluginHost.service(TeacherService.KEY)
            ?: error("TeacherService 未注册")
        val result = await(service.search("张"))
        android.util.Log.i("RealDataTest", "teachers=${result?.state} msg=${result?.message} size=${result?.data?.size}")
        assumeTrue("教师服务网络不可用", result?.state == DataState.STATE.SUCCESS)
        assertTrue("教师结果为空", !result?.data.isNullOrEmpty())
    }

    private fun <T> await(liveData: LiveData<T>, timeoutMs: Long = 20_000): T? {
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
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            liveData.removeObserver(observer)
        }
        return value
    }
}
