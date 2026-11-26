package com.stupidtree.hita.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TimetableDataContractTest {

    @Test
    fun keysRemainStable() {
        assertEquals("hita.timetable", TimetableService.KEY.name)
        assertEquals("hita.timetable.data", TimetableDataService.KEY.name)
    }

    @Test
    fun dataServiceDoesNotExposeRoomLiveDataOrPlatformStreams() {
        val forbidden = listOf(
            "androidx.lifecycle.LiveData",
            "androidx.room.",
            "java.sql.",
            "java.util.Calendar",
            "java.io.InputStream",
            "android.content.",
            "androidx.fragment.",
        )
        val methods = TimetableDataService::class.java.methods
            .filterNot { it.declaringClass == Any::class.java }
        methods.forEach { method ->
            val types = listOf(method.returnType) + method.parameterTypes
            types.forEach { type ->
                forbidden.forEach { token ->
                    assertFalse(
                        "${method.name} exposes $token",
                        type.name.contains(token),
                    )
                }
            }
        }
    }
}
