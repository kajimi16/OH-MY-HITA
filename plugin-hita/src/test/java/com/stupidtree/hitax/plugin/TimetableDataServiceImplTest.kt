package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.TimetableDataService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.dto.TimetableDefaults
import com.stupidtree.hita.api.dto.TimetableDto
import com.stupidtree.hita.api.dto.DtoJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableDataServiceImplTest {

    @Test
    fun legacyServiceStillImplementsOldContract() {
        assertEquals("hita.timetable", TimetableService.KEY.name)
        assertEquals("hita.timetable.data", TimetableDataService.KEY.name)
        assertTrue(TimetableService::class.java.isAssignableFrom(TimetableServiceImpl::class.java))
        assertTrue(TimetableDataService::class.java.isAssignableFrom(TimetableDataServiceImpl::class.java))
    }

    @Test
    fun invalidRangesAndLimitsAreExplicitErrors() {
        assertThrows(IllegalArgumentException::class.java) { requireValidEventRange(2, 1) }
        assertThrows(IllegalArgumentException::class.java) { requireValidLimit(-1) }
        assertThrows(IllegalArgumentException::class.java) { requireValidPosition(-1) }
        assertThrows(IllegalArgumentException::class.java) { requireValidId("", "timetable") }
        assertThrows(IllegalArgumentException::class.java) { requireValidId("   ", "event") }
    }

    @Test
    fun emptyListsAreValidInputsForHelpers() {
        // 空列表由实现直接 no-op；这里固定校验函数不误伤 0。
        requireValidLimit(0)
        requireValidPosition(0)
        requireValidEventRange(100, 100)
    }

    @Test
    fun observeSubscriptionDoesNotEmitAfterClose() {
        var closes = 0
        var calls = 0
        val subscription = ChangeSubscription { closes++ }

        subscription.emit { calls++ }
        subscription.close()
        subscription.emit { calls++ }

        assertEquals(1, calls)
        assertEquals(1, closes)
        subscription.close()
        assertEquals(1, closes)
    }

    @Test
    fun timetableDtoRoundTripsThroughJson() {
        val dto = TimetableDto(
            id = "t1",
            name = "2026 春",
            code = "2026-1",
            startTime = 1_744_000_000_123L,
            endTime = 1_746_000_000_456L,
            createdAt = 1_743_000_000_789L,
            periods = TimetableDefaults.periods(),
        )
        val back = DtoJson.fromJson(DtoJson.toJson(dto), TimetableDto::class.java)
        assertEquals(dto, back)
        assertFalse(back.periods.isEmpty())
    }

    // JUnit 4.13 的 assertThrows 在 Kotlin 下用块语法更直接。
    private fun assertThrows(expected: Class<out Throwable>, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            if (expected.isInstance(t)) return
            throw AssertionError("expected ${expected.name}, got ${t::class.java.name}", t)
        }
        throw AssertionError("expected ${expected.name}, but nothing was thrown")
    }
}
