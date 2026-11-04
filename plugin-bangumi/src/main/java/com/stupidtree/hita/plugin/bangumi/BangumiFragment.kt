package com.stupidtree.hita.plugin.bangumi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.concurrent.Executors

/** 番剧页：默认展示今日放送，支持关键词搜索。 */
class BangumiFragment : Fragment(R.layout.fragment_bangumi) {

    private lateinit var searchInput: EditText
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var list: RecyclerView
    private lateinit var status: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private val adapter = BangumiAdapter { anime ->
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bgm.tv/subject/${anime.id}")))
    }
    private var query: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchInput = view.findViewById(R.id.search_input)
        swipe = view.findViewById(R.id.swipe)
        list = view.findViewById(R.id.list)
        status = view.findViewById(R.id.status)
        val searchButton: Button = view.findViewById(R.id.search_button)

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        swipe.setOnRefreshListener { load(query) }
        searchButton.setOnClickListener { submitSearch() }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else {
                false
            }
        }
        load(query)
    }

    private fun submitSearch() {
        query = searchInput.text.toString().trim()
        load(query)
    }

    private fun load(keyword: String) {
        swipe.isRefreshing = true
        status.visibility = View.GONE
        executor.execute {
            val result = runCatching {
                if (keyword.isBlank()) BangumiRepository.today() else BangumiRepository.search(keyword)
            }
            val currentView = view ?: return@execute
            currentView.post {
                swipe.isRefreshing = false
                result.onSuccess { items ->
                    adapter.submit(items)
                    if (items.isEmpty()) {
                        status.text = getString(R.string.bangumi_empty)
                        status.visibility = View.VISIBLE
                    }
                }.onFailure {
                    status.text = getString(R.string.bangumi_error, it.message ?: "")
                    status.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        executor.shutdownNow()
        super.onDestroyView()
    }
}
