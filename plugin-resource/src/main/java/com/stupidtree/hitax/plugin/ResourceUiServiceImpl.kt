package com.stupidtree.hitax.plugin

import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hita.api.RuntimeUiService
import com.stupidtree.hitax.data.repository.TeacherInfoRepository
import com.stupidtree.hitax.ui.resource.ResourceNavigator
import com.stupidtree.hitax.utils.CourseResourceLinker

class ResourceUiServiceImpl(
    private val application: Application,
    /** 用户装了对应的运行时界面插件时优先交给它；缺席则走内置页面。 */
    private val runtimeUi: RuntimeUiService? = null,
) : ResourceUiService {

    override fun openSearch(context: Context, keyword: String?, submit: Boolean) {
        val args = android.os.Bundle().apply {
            putString("query", keyword)
            putBoolean("submit", submit)
        }
        val capability = if (submit) {
            RuntimeUiService.CAP_RESOURCE_CONTRIBUTE
        } else {
            RuntimeUiService.CAP_RESOURCE_SEARCH
        }
        if (runtimeUi?.open(context, capability, args) == true) return
        ResourceNavigator.startSearch(
            context,
            keyword,
            if (submit) ResourceNavigator.CourseResourceMode.SUBMIT
            else ResourceNavigator.CourseResourceMode.VIEW,
        )
    }

    override fun openReadme(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        courseCode: String?,
        courseName: String?,
    ) {
        CourseResourceLinker.openReadme(context, lifecycleOwner, courseCode, courseName)
    }

    override fun openContribution(
        context: Context,
        repoName: String,
        courseName: String,
        courseCode: String,
        repoType: String,
    ) {
        ResourceNavigator.startContribution(context, repoName, courseName, courseCode, repoType)
    }

    override fun openTeacherHomepage(context: Context, teacherName: String) {
        ResourceNavigator.startTeacherHomepageSearch(context, teacherName)
    }

    override fun searchTeacherNames(query: String): LiveData<List<String>> {
        val out = MediatorLiveData<List<String>>()
        out.addSource(
            TeacherInfoRepository.getInstance(application).searchTeachers(query)
        ) { state -> out.value = state.data?.map { it.name } ?: emptyList() }
        return out
    }
}
