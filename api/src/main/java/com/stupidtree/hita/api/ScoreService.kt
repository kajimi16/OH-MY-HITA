package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.TermItem

/** 单门成绩。 */
data class ScoreEntry(
    val courseName: String,
    val credits: Int,
    val score: Int,
    val termName: String? = null,
)

/** 一个学期的成绩汇总。 */
data class ScoreReport(
    val term: TermItem,
    val gpa: String,
    val rank: String,
    val total: String,
    val entries: List<ScoreEntry>,
)

/**
 * 教务成绩查询（API 7）。
 * 由 `plugin-eas` 提供；未登录或插件缺失时返回 NOT_LOGGED_IN / 空。
 */
interface ScoreService {

    fun terms(): LiveData<DataState<List<TermItem>>>

    fun report(term: TermItem, includeResit: Boolean = false): LiveData<DataState<ScoreReport>>

    companion object {
        val KEY = ServiceKey<ScoreService>("hita.eas.scores")
    }
}
