package com.stupidtree.hitax.market

import com.stupidtree.hitax.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinTabIconsTest {

    @Test
    fun parsesPrefixedName() {
        assertEquals("check", BuiltinTabIcons.parseName("builtin:check"))
        assertEquals("toys", BuiltinTabIcons.parseName("  builtin:toys  "))
        assertTrue(BuiltinTabIcons.isBuiltin("builtin:today"))
    }

    @Test
    fun rejectsNonBuiltinAndBlankNames() {
        assertNull(BuiltinTabIcons.parseName(null))
        assertNull(BuiltinTabIcons.parseName(""))
        assertNull(BuiltinTabIcons.parseName("appicon.png"))
        assertNull(BuiltinTabIcons.parseName("builtin:"))
        assertNull(BuiltinTabIcons.parseName("builtin:   "))
        assertFalse(BuiltinTabIcons.isBuiltin("ic_launcher"))
    }

    @Test
    fun resolvesKnownNamesToHostDrawables() {
        assertEquals(R.drawable.ic_baseline_check_24, BuiltinTabIcons.resolve("builtin:check"))
        assertEquals(R.drawable.ic_baseline_toys_24, BuiltinTabIcons.resolve("builtin:toys"))
        assertEquals(R.drawable.ic_nav_today, BuiltinTabIcons.resolve("builtin:today"))
        assertEquals(R.drawable.ic_nav_timetable, BuiltinTabIcons.resolve("builtin:timetable"))
        assertEquals(R.drawable.ic_nav_navigation, BuiltinTabIcons.resolve("builtin:navigation"))
        assertEquals(R.drawable.ic_nav_blog, BuiltinTabIcons.resolve("builtin:blog"))
    }

    @Test
    fun unknownNameFallsBackToNull() {
        assertNull(BuiltinTabIcons.resolve("builtin:not_a_tab"))
        assertNull(BuiltinTabIcons.resolve("appicon.png"))
    }

    @Test
    fun everyAvailableNameResolves() {
        assertTrue(BuiltinTabIcons.available().isNotEmpty())
        BuiltinTabIcons.available().forEach { name ->
            assertNotNull("内置图标名 $name 解析失败", BuiltinTabIcons.resolve("builtin:$name"))
        }
    }
}
