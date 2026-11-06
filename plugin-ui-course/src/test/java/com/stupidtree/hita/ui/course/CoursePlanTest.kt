package com.stupidtree.hita.ui.course

import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogItem
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogSource
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseMeeting
import org.junit.Assert.assertEquals
import org.junit.Test

class CoursePlanTest {
    private fun course(
        id: String,
        credits: String,
        weekday: Int,
        begin: Int,
        end: Int,
        weeks: List<Int> = listOf(1, 2, 3),
    ) = ShenzhenCourseCatalogItem(
        id = id,
        courseCode = id,
        courseName = "课程$id",
        credits = credits,
        source = ShenzhenCourseCatalogSource.SCHOOL,
        meetings = listOf(ShenzhenCourseMeeting(weeks, weekday, begin, end)),
    )

    @Test
    fun creditsSum() {
        assertEquals(5.5, CoursePlan.credits(listOf(course("A", "2.5", 1, 1, 2), course("B", "3", 2, 1, 2))), 0.01)
    }

    @Test
    fun detectsMeetingOverlap() {
        val a = course("A", "2", 1, 1, 2)
        val b = course("B", "2", 1, 2, 3)
        assertEquals(1, CoursePlan.conflicts(listOf(a, b)).size)
    }

    @Test
    fun ignoresDifferentDayOrWeeks() {
        val a = course("A", "2", 1, 1, 2)
        val b = course("B", "2", 2, 1, 2)
        val c = course("C", "2", 1, 1, 2, weeks = listOf(4, 5))
        assertEquals(0, CoursePlan.conflicts(listOf(a, b)).size)
        assertEquals(0, CoursePlan.conflicts(listOf(a, c)).size)
    }
}
