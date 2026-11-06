package com.stupidtree.hita.ui.course

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogItem
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogSource
import org.junit.Assert.assertEquals
import org.junit.Test

class CoursePlanPersistenceTest {
    @Test
    fun gsonRoundTripKeepsSelectedCourses() {
        val items = listOf(
            ShenzhenCourseCatalogItem(
                id = "c1",
                courseCode = "CS101",
                courseName = "计算机导论",
                credits = "3",
                source = ShenzhenCourseCatalogSource.AVAILABLE,
            ),
        )
        val json = Gson().toJson(items)
        val restored = Gson().fromJson<List<ShenzhenCourseCatalogItem>>(
            json,
            object : TypeToken<List<ShenzhenCourseCatalogItem>>() {}.type,
        )
        assertEquals(1, restored.size)
        assertEquals("CS101", restored[0].courseCode)
        assertEquals("3", restored[0].credits)
    }
}
