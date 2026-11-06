package com.stupidtree.hitax.eas

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.CourseCatalogService
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogPage
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogSource
import com.stupidtree.hitax.data.model.eas.ShenzhenSelectionPools
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.source.web.eas.EASource
import com.stupidtree.hitax.data.source.web.eas.ShenzhenCourseCatalogParser

internal class CourseCatalogServiceImpl(private val application: Application) : CourseCatalogService {
    private val repo get() = EASRepository.getInstance(application)
    private val source = EASource()

    override fun available(
        term: TermItem,
        poolCode: String,
        keyword: String,
        page: Int,
        pageSize: Int,
    ): LiveData<DataState<ShenzhenCourseCatalogPage>> = query(
        term, keyword, page, pageSize, poolCode, available = true,
    )

    override fun school(
        term: TermItem,
        keyword: String,
        page: Int,
        pageSize: Int,
    ): LiveData<DataState<ShenzhenCourseCatalogPage>> = query(
        term, keyword, page, pageSize, "", available = false,
    )

    private fun query(
        term: TermItem,
        keyword: String,
        page: Int,
        pageSize: Int,
        poolCode: String,
        available: Boolean,
    ): LiveData<DataState<ShenzhenCourseCatalogPage>> {
        val result = MutableLiveData<DataState<ShenzhenCourseCatalogPage>>()
        Thread {
            val token = repo.getEasToken()
            if (token.cookies.isEmpty()) {
                result.postValue(DataState(DataState.STATE.NOT_LOGGED_IN, "请先连接深圳 Web 教务"))
                return@Thread
            }
            runCatching {
                val mode = if (available) "1" else token.getStudentType()
                val form = if (available) availableForm(term, mode, keyword, page, pageSize, poolCode)
                else schoolForm(term, mode, keyword, page, pageSize)
                if (available) {
                    val init = source.jwPost(
                        token, "/Xsxk/queryYxkc?sf_request_type=ajax", form, "/Xsxk/query/1",
                    )
                    if (init.statusCode() == 401 || init.statusCode() == 302) error("深圳 Web 会话已失效")
                }
                val path = if (available) "/Xsxk/queryKxrw?sf_request_type=ajax"
                else "/Xsxktz/queryRwxxcxList?sf_request_type=ajax"
                val referer = if (available) "/Xsxk/query/1" else "/Xsxktz/queryRwxxcx"
                val response = source.jwPost(token, path, form, referer)
                if (response.statusCode() == 401 || response.statusCode() == 302) error("深圳 Web 会话已失效")
                if (response.statusCode() != 200) error("课程目录请求失败")
                val poolName = ShenzhenSelectionPools.all.firstOrNull { it.code == poolCode }?.name.orEmpty()
                ShenzhenCourseCatalogParser.parsePage(
                    response.body(),
                    if (available) ShenzhenCourseCatalogSource.AVAILABLE else ShenzhenCourseCatalogSource.SCHOOL,
                    mode,
                    poolName,
                ) ?: error("课程目录解析失败")
            }.onSuccess { result.postValue(DataState(it, DataState.STATE.SUCCESS)) }
                .onFailure { result.postValue(DataState(DataState.STATE.FETCH_FAILED, it.message)) }
        }.start()
        return result
    }

    private fun availableForm(
        term: TermItem, studentType: String, keyword: String, page: Int, pageSize: Int, pool: String,
    ) = linkedMapOf(
        "cxsfmt" to "0", "p_pylx" to studentType, "mxpylx" to studentType,
        "p_sfgldjr" to "0", "p_sfredis" to "0", "p_sfsyxkgwc" to "0",
        "p_xktjz" to "", "p_chaxunxh" to "", "p_gjz" to keyword, "p_skjs" to "",
        "p_xn" to term.yearCode, "p_xq" to term.termCode, "p_xnxq" to term.getCode(),
        "p_xkfsdm" to pool, "p_xiaoqu" to "", "p_kkyx" to "", "p_kclb" to "",
        "p_xkxs" to "", "p_dyc" to "", "p_kkxnxq" to "", "p_id" to "",
        "p_sfhlctkc" to "0", "p_sfhllrlkc" to "0", "p_kxsj_xqj" to "",
        "p_kxsj_ksjc" to "", "p_kxsj_jsjc" to "", "p_kcdm_js" to "",
        "p_kcdm_cxrw" to "", "p_kcdm_cxrw_zckc" to "", "p_kc_gjz" to keyword,
        "p_xzcxtjz_nj" to "", "p_xzcxtjz_yx" to "", "p_xzcxtjz_zy" to "",
        "p_xzcxtjz_zyfx" to "", "p_xzcxtjz_bj" to "", "p_sfxsgwckb" to "1",
        "p_skyy" to "", "p_sfmxzj" to "0", "p_chaxunxkfsdm" to "",
        "pageNum" to page.toString(), "pageSize" to pageSize.toString(),
    )

    private fun schoolForm(
        term: TermItem, studentType: String, keyword: String, page: Int, pageSize: Int,
    ) = linkedMapOf(
        "p_chapylx" to "", "ordertext_0" to "", "p_xn" to term.yearCode,
        "p_xq" to term.termCode, "p_xnxq" to term.getCode(), "p_gjz" to keyword,
        "p_xiaoqu" to "", "p_kkyx" to "", "p_rwlx" to "", "p_kclb" to "",
        "p_kcxz" to "", "p_chaxungjz" to keyword, "p_chaxunxiaoqu" to "",
        "p_chaxunkkyx" to "", "p_chaxunnj" to "", "p_chaxunglyx" to "",
        "p_chaxunzy" to "", "p_chaxunxdm" to "", "p_chaxunpylx" to studentType,
        "mxpylx" to studentType, "p_zc" to "", "p_xqj" to "", "p_ksjc" to "",
        "p_jsjc" to "", "p_skjs" to "", "p_ids" to "", "p_id" to "",
        "p_sfhltsxx" to "0", "file" to "",
        "pageNum" to page.toString(), "pageSize" to pageSize.toString(),
    )
}
