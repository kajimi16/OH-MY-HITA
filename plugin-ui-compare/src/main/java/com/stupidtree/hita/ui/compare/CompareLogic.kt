package com.stupidtree.hita.ui.compare

import com.stupidtree.hitax.data.model.timetable.EventItem

internal object CompareLogic {
    fun overlaps(a: List<EventItem>, b: List<EventItem>): List<Pair<EventItem, EventItem>> =
        a.flatMap { left ->
            b.filter { right ->
                left.from.time < right.to.time && right.from.time < left.to.time
            }.map { right -> left to right }
        }
}
