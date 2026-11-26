package com.stupidtree.hita.api.dto

import com.google.gson.annotations.SerializedName
import com.stupidtree.hitax.data.model.timetable.Timetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ContractPurityTest {

    private val dtoClasses: List<Class<*>> = listOf(
        EasAccountDto::class.java,
        EasLoginRequestDto::class.java,
        EasLoginResultDto::class.java,
        EasReimportResultDto::class.java,
        EasImportOptionsDto::class.java,
        TimetableDto::class.java,
        TimePeriodDto::class.java,
        EventDto::class.java,
        SubjectDto::class.java,
        SubjectColorDto::class.java,
        TeacherDto::class.java,
        SubjectProgressDto::class.java,
        TimetableSnapshotDto::class.java,
        TimetableChangeDto::class.java,
        IcsExportResultDto::class.java,
        IcsImportRequestDto::class.java,
        IcsImportResultDto::class.java,
        CatalogCourseDto::class.java,
    )

    @Test
    fun dtoFieldsOnlyUseSerializableScalarsAndDtos() {
        dtoClasses.forEach { clazz ->
            clazz.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .forEach { field ->
                    assertTrue(
                        "${clazz.simpleName}.${field.name} has forbidden type ${field.genericType}",
                        isAllowed(field.genericType),
                    )
                }
        }
    }

    @Test
    fun everyDtoFieldHasFixedWireName() {
        dtoClasses.forEach { clazz ->
            clazz.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .forEach { field ->
                    val wire = wireName(clazz, field)
                    assertNotNull("${clazz.simpleName}.${field.name} missing @SerializedName", wire)
                }
        }
    }

    @Test
    fun subjectCountInSpaUsesLegacyWireName() {
        val field = SubjectDto::class.java.getDeclaredField("countInSpa")
        val annotation = field.getAnnotation(SerializedName::class.java)
        assertNotNull(annotation)
        assertEquals("countInSPA", annotation!!.value)
    }

    @Test
    fun accountDtoHasNoCredentialFields() {
        val names = EasAccountDto::class.java.declaredFields
            .map { it.name.lowercase() }
            .toSet()
        listOf("accesstoken", "refreshtoken", "password", "cookies", "webcookies")
            .forEach { secret ->
                assertFalse("EasAccountDto must not expose $secret", names.contains(secret))
            }
    }

    @Test
    fun defaultPeriodsMatchTimetableModel() {
        val model = Timetable().getDefaultTimeStructure()
        val dto = TimetableDefaults.periods()

        assertEquals(12, dto.size)
        assertEquals(model.size, dto.size)
        model.zip(dto).forEach { (entity, period) ->
            assertEquals(entity.from.hour, period.fromHour)
            assertEquals(entity.from.minute, period.fromMinute)
            assertEquals(entity.to.hour, period.toHour)
            assertEquals(entity.to.minute, period.toMinute)
        }
    }

    private fun wireName(clazz: Class<*>, field: Field): String? {
        val annotation = field.getAnnotation(SerializedName::class.java) ?: return null
        if (annotation.value.isBlank()) return null
        return "${clazz.simpleName}.${field.name} -> ${annotation.value}"
    }

    private fun isAllowed(type: Type): Boolean {
        if (type is Class<*>) {
            if (type.isPrimitive) return true
            if (type == String::class.java) return true
            if (type == ByteArray::class.java) return true
            return dtoClasses.contains(type)
        }
        if (type is ParameterizedType) {
            val raw = type.rawType as? Class<*> ?: return false
            if (raw != List::class.java) return false
            return type.actualTypeArguments.all { isAllowed(it) }
        }
        return false
    }
}
