package com.stupidtree.hita.api

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

/**
 * 课程资源 / 教师检索插件的界面入口。
 *
 * HOA 数据源、教师抓取与相关页面都在插件内，宿主只认这个契约。
 */
interface ResourceUiService {
    fun openSearch(context: Context, keyword: String?, submit: Boolean = false)

    fun openReadme(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        courseCode: String?,
        courseName: String?,
    )

    fun openContribution(
        context: Context,
        repoName: String,
        courseName: String,
        courseCode: String,
        repoType: String,
    )

    fun openTeacherHomepage(context: Context, teacherName: String)

    /** 教师名自动补全，供添加事件弹窗使用。 */
    fun searchTeacherNames(query: String): LiveData<List<String>>

    companion object {
        val KEY = ServiceKey<ResourceUiService>("hita.resource.ui")
    }
}
