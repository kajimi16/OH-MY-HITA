package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState

/** 社区话题/文章摘要；T2 只消费该 DTO。 */
data class ThetaTopic(
    val id: String,
    val name: String,
    val description: String,
    val avatar: String,
    val articleNum: Int,
)

data class ThetaComment(
    val id: String,
    val content: String,
    val author: String,
    val createTime: Long,
)

data class ThetaArticle(
    val id: String,
    val title: String,
    val content: String,
    val author: String,
    val createTime: Long,
)

interface ThetaService {
    fun topics(mode: String, page: Int): LiveData<DataState<List<ThetaTopic>>>
    fun articles(topicId: String, page: Int): LiveData<DataState<List<ThetaArticle>>>

    fun comments(articleId: String, page: Int): LiveData<DataState<List<ThetaComment>>>

    companion object { val KEY = ServiceKey<ThetaService>("hita.theta.feed") }
}
