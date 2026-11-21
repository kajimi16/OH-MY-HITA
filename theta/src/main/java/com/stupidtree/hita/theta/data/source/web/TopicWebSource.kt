package com.stupidtree.hita.theta.data.source.web

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.stupidtree.component.data.DataState
import com.stupidtree.component.web.ApiResponse
import com.stupidtree.component.web.BaseWebSource
import com.stupidtree.hita.theta.data.model.Topic
import com.stupidtree.hita.theta.data.source.web.service.TopicService
import com.stupidtree.stupiduser.data.source.web.service.codes
import com.stupidtree.stupiduser.data.source.web.service.codes.SUCCESS
import com.stupidtree.stupiduser.util.HttpUtils
import java.util.*

/**
 * 层次：DataSource
 * 用户的数据源
 * 类型：网络数据
 * 数据：异步读，异步写
 */
class TopicWebSource(context: Context) : BaseWebSource<TopicService>(
    context
) {
    override fun getServiceClass(): Class<TopicService> {
        return TopicService::class.java
    }

    fun getTopics(
        token: String,
        mode: String,
        pageSize: Int,
        pageNum: Int,
        extra: String
    ): LiveData<DataState<List<Topic>>> {
        val result = MediatorLiveData<DataState<List<Topic>>>()
        result.addSource(
            service.getTopics(HttpUtils.getHeaderAuth(token), mode, pageSize, pageNum, extra),
            object : Observer<ApiResponse<List<Topic>>?> {
                override fun onChanged(input: ApiResponse<List<Topic>>?) {
                    if (input == null) return
                    result.value = when (input.code) {
                        SUCCESS -> DataState(input.data ?: listOf())
                        codes.TOKEN_INVALID -> DataState(DataState.STATE.TOKEN_INVALID)
                        else -> DataState(DataState.STATE.FETCH_FAILED, input.message)
                    }
                }
            },
        )
        return result
    }

    fun getTopic(
        token: String,
        topicId: String
    ): LiveData<DataState<Topic>> {
        val result = MediatorLiveData<DataState<Topic>>()
        result.addSource(
            service.getTopic(HttpUtils.getHeaderAuth(token), topicId),
            object : Observer<ApiResponse<Topic>?> {
                override fun onChanged(input: ApiResponse<Topic>?) {
                    if (input == null) return
                    result.value = when (input.code) {
                        SUCCESS -> DataState(input.data ?: Topic())
                        codes.TOKEN_INVALID -> DataState(DataState.STATE.TOKEN_INVALID)
                        else -> DataState(DataState.STATE.FETCH_FAILED, input.message)
                    }
                }
            },
        )
        return result
    }


    companion object {
        var instance: TopicWebSource? = null
        fun getInstance(context: Context): TopicWebSource {
            synchronized(TopicWebSource::class.java) {
                if (instance == null) {
                    instance = TopicWebSource(context.applicationContext)
                }
                return instance!!
            }
        }
    }

}