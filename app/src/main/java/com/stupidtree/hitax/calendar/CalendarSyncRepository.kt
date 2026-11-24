package com.stupidtree.hitax.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.stupidtree.hitax.data.model.timetable.EventItem
import java.util.TimeZone

class CalendarSyncRepository(private val context: Context) {

    sealed interface Result {
        data class Success(val inserted: Int, val updated: Int, val deleted: Int) : Result
        data object PermissionDenied : Result
        data object NoWritableCalendar : Result
        data class Failed(val message: String) : Result
    }

    fun sync(
        timetableId: String,
        timetableName: String?,
        events: List<EventItem>,
    ): Result {
        if (!hasPermission(Manifest.permission.READ_CALENDAR) ||
            !hasPermission(Manifest.permission.WRITE_CALENDAR)
        ) {
            return Result.PermissionDenied
        }
        return runCatching {
            val calendarId = writableCalendarId() ?: return Result.NoWritableCalendar
            val existing = existingEvents(calendarId, timetableId)
            val entries = CalendarSyncPlanner.entries(events, timetableId, timetableName)
            val plan = CalendarSyncPlanner.plan(entries, existing.keys)
            val resolver = context.contentResolver
            plan.inserts.forEach { entry ->
                resolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    entry.toValues(calendarId),
                )
            }
            plan.updates.forEach { entry ->
                val id = existing[entry.key] ?: return@forEach
                resolver.update(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                    entry.toValues(calendarId),
                    null,
                    null,
                )
            }
            plan.deletes.forEach { key ->
                val id = existing[key] ?: return@forEach
                resolver.delete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                    null,
                    null,
                )
            }
            Result.Success(plan.inserts.size, plan.updates.size, plan.deletes.size)
        }.getOrElse { error ->
            if (error is SecurityException) Result.PermissionDenied
            else Result.Failed(error.message ?: error::class.java.simpleName)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun writableCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND " +
                "${CalendarContract.Calendars.VISIBLE} = 1",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars.IS_PRIMARY} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    private fun existingEvents(calendarId: Long, timetableId: String): Map<String, Long> {
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DESCRIPTION)
        val result = LinkedHashMap<String, Long>()
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "%hita:$timetableId:%"),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val key = CalendarSyncPlanner.keyFromDescription(cursor.getString(1)) ?: continue
                result[key] = cursor.getLong(0)
            }
        }
        return result
    }

    private fun CalendarSyncEntry.toValues(calendarId: Long): ContentValues =
        ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DESCRIPTION, description + "\n" + key)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
}

