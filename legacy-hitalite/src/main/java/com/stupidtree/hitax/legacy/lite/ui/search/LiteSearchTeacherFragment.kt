package com.stupidtree.hitax.legacy.lite.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.TeacherSearchResult
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteDynamicTeacherSearchResultItemBinding
import com.stupidtree.hitax.legacy.lite.databinding.LiteFragmentSearchResult1Binding

/**
 * 教师检索结果页。视觉按 Lite fragment_search_result_1；服务来自宿主 TeacherService。
 */
class LiteSearchTeacherFragment : Fragment() {

    private var binding: LiteFragmentSearchResult1Binding? = null
    private val adapter = TeacherAdapter()
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = LiteFragmentSearchResult1Binding.inflate(inflater, container, false)
        binding = b
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        b.refresh.setOnRefreshListener { search(currentQuery) }
        return b.root
    }

    fun search(text: String) {
        currentQuery = text
        if (text.isBlank()) {
            adapter.submit(emptyList())
            binding?.result?.text = ""
            return
        }
        LiteHostBridge.teacher?.search(text)?.observe(viewLifecycleOwner) { state ->
            binding?.refresh?.isRefreshing = false
            if (state.state == DataState.STATE.SUCCESS) {
                val items = state.data.orEmpty()
                adapter.submit(items)
                binding?.result?.text =
                    if (items.isEmpty()) getString(R.string.lite_search_nothing_found)
                    else getString(R.string.lite_search_teacher_total, items.size)
            } else if (state.state != DataState.STATE.NOTHING) {
                adapter.submit(emptyList())
                binding?.result?.setText(R.string.lite_search_fail)
            }
        }
    }

    private inner class TeacherAdapter : RecyclerView.Adapter<TeacherHolder>() {
        private val items = mutableListOf<TeacherSearchResult>()

        fun submit(data: List<TeacherSearchResult>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherHolder =
            TeacherHolder(
                LiteDynamicTeacherSearchResultItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: TeacherHolder, position: Int) {
            val item = items[position]
            holder.binding.title.text = item.name
            holder.binding.subtitle.text = item.department
            holder.binding.tag.visibility = View.GONE
            holder.binding.picture.setImageResource(R.drawable.lite_place_holder_avatar)
        }
    }

    private class TeacherHolder(val binding: LiteDynamicTeacherSearchResultItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
