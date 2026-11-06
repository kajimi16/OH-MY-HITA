package com.stupidtree.hita.ui.course

import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogItem
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseMeeting

internal object CoursePlan {

    fun credits(items: List<ShenzhenCourseCatalogItem>): Double =
        items.sumOf { it.credits.toDoubleOrNull() ?: 0.0 }

    fun conflicts(items: List<ShenzhenCourseCatalogItem>): List<Pair<ShenzhenCourseCatalogItem, ShenzhenCourseCatalogItem>> =
        buildList {
            for (i in items.indices) {
                for (j in i + 1 until items.size) {
                    if (overlaps(items[i].meetings, items[j].meetings)) add(items[i] to items[j])
                }
            }
        }

    private fun overlaps(a: List<ShenzhenCourseMeeting>, b: List<ShenzhenCourseMeeting>): Boolean =
        a.any { left -> b.any { right -> meetingOverlap(left, right) } }

    private fun meetingOverlap(a: ShenzhenCourseMeeting, b: ShenzhenCourseMeeting): Boolean {
        if (a.weekday != b.weekday) return false
        if (a.beginPeriod > b.endPeriod || b.beginPeriod > a.endPeriod) return false
        return a.weeks.toSet().intersect(b.weeks.toSet()).isNotEmpty()
    }
}
