package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogPage
import com.stupidtree.hitax.data.model.eas.TermItem

/** 深圳教务选课目录（只读，API 7）。 */
interface CourseCatalogService {
    fun available(
        term: TermItem,
        poolCode: String,
        keyword: String = "",
        page: Int = 1,
        pageSize: Int = 20,
    ): LiveData<DataState<ShenzhenCourseCatalogPage>>

    fun school(
        term: TermItem,
        keyword: String = "",
        page: Int = 1,
        pageSize: Int = 20,
    ): LiveData<DataState<ShenzhenCourseCatalogPage>>

    companion object {
        val KEY = ServiceKey<CourseCatalogService>("hita.eas.courseCatalog")
    }
}
