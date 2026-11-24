package com.stupidtree.hita.plugin.todo.model

/** A platform's published, unsubmitted item. Completion is local; the deadline is an epoch millisecond. */
internal data class ImportedTodo(
    val id: String,
    val title: String,
    val courseTitle: String,
    val dueAt: Long,
    val completed: Boolean,
)
