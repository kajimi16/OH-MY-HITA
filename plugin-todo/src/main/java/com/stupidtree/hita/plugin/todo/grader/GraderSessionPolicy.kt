package com.stupidtree.hita.plugin.todo.grader

internal object GraderSessionPolicy {
    const val ORIGIN = "https://grader.hitsz.edu.cn"

    /** Ignore every other browser cookie; never accept a header containing control bytes. */
    fun sessionCookie(header: String): String? {
        if (header.any { it.code < 0x20 || it.code > 0x7e }) return null
        val matches = header.split(';').map { it.trim() }.filter { it.substringBefore('=') == "sessionid" }
        val value = matches.singleOrNull()?.substringAfter('=', "") ?: return null
        if (value.isEmpty() || value.any { it == '"' || it == ',' || it == ';' || it == '\\' || it == ' ' }) return null
        return "sessionid=$value"
    }
}
