package com.stupidtree.hitax.replica.cakeni

import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.Timetable

object CakeniSource {
    fun loadWeek(
        service: TimetableService?,
        timetable: Timetable,
        week: Int,
        dayCount: Int,
    ): CakeniWeek {
        if (service == null) return buildCakeniWeek(emptyList(), timetable, week, dayCount)
        val start = weekStart(timetable, week)
        val events = runCatching {
            service.getEventsDuringSync(start, start + 7L * 24 * 60 * 60 * 1000)
        }.getOrDefault(emptyList())
        return buildCakeniWeek(events, timetable, week, dayCount)
    }
}
