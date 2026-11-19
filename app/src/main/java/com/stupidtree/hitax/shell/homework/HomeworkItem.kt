package com.stupidtree.hitax.shell.homework

data class HomeworkItem(
    val id: String,
    val title: String,
    val createdAt: Long,
    val completed: Boolean = false,
    /** Local calendar date (ISO yyyy-MM-dd); null preserves undated legacy tasks. */
    val plannedDate: String? = null,
)
