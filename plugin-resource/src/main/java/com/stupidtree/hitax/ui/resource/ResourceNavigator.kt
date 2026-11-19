package com.stupidtree.hitax.ui.resource

import android.content.Context
import android.content.Intent
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** 插件内部的页面跳转，替代宿主 ActivityUtils 中的同名入口。 */
object ResourceNavigator {

    enum class CourseResourceMode { VIEW, SUBMIT }

    fun startSearch(
        context: Context,
        query: String? = null,
        mode: CourseResourceMode = CourseResourceMode.VIEW,
    ) {
        context.startActivity(
            Intent(context, CourseResourceSearchActivity::class.java)
                .putExtra("query", query)
                .putExtra("mode", mode.name)
        )
    }

    fun startReadme(
        context: Context,
        repoName: String,
        courseName: String,
        courseCode: String,
        repoType: String = "normal",
    ) {
        context.startActivity(
            Intent(context, CourseReadmeActivity::class.java)
                .putExtra("repoName", repoName)
                .putExtra("courseName", courseName)
                .putExtra("courseCode", courseCode)
                .putExtra("repoType", repoType)
        )
    }

    fun startContribution(
        context: Context,
        repoName: String,
        courseName: String,
        courseCode: String,
        repoType: String = "normal",
    ) {
        context.startActivity(
            Intent(context, CourseContributionActivity::class.java)
                .putExtra("repoName", repoName)
                .putExtra("courseName", courseName)
                .putExtra("courseCode", courseCode)
                .putExtra("repoType", repoType)
        )
    }

    fun startInternalWeb(context: Context, title: String, url: String) {
        context.startActivity(
            Intent(context, InternalWebActivity::class.java)
                .putExtra("title", title)
                .putExtra("url", url)
        )
    }

    fun startTeacherHomepageSearch(context: Context, teacherName: String) {
        if (teacherName.isBlank()) return
        val encoded = URLEncoder.encode(teacherName, StandardCharsets.UTF_8.toString())
        startInternalWeb(
            context,
            teacherName,
            "https://homepage.hit.edu.cn/search-teacher-by-phoneticize?condition=$encoded",
        )
    }
}
