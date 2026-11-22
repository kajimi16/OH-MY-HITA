package com.stupidtree.hitax.legacy.lite.ui.eas.exam

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.ExamItem
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteExamActivityBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter

/**
 * HITA_Lite 考试查询页。
 *
 * 布局与 Lite 2.1 的考试查询页一致；数据全部来自宿主
 * [com.stupidtree.hita.api.ExamService]，不复制旧数据访问层。
 */
class LiteExamActivity :
    BaseActivity<LiteExamViewModel, LiteExamActivityBinding>() {

    private lateinit var listAdapter: LiteExamListAdapter
    private var loginRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
        binding.collapse.title = getString(R.string.lite_exam_label)
    }

    override fun initViewBinding(): LiteExamActivityBinding =
        LiteExamActivityBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteExamViewModel> = LiteExamViewModel::class.java

    override fun initViews() {
        bindLiveData()
        binding.refresh.setColorSchemeColors(getColorPrimary())
        binding.refresh.setOnRefreshListener { refresh() }
        listAdapter = LiteExamListAdapter(this, mutableListOf())
        binding.examStructure.adapter = listAdapter
        binding.examStructure.layoutManager = LinearLayoutManager(this)
        listAdapter.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<ExamItem> {
            override fun onItemClick(data: ExamItem?, card: View?, position: Int) {
                data?.let {
                    LiteExamDetailFragment.newInstance(it)
                        .show(supportFragmentManager, "exam_detail")
                }
            }
        })
        loadAfterLogin()
    }

    private fun loadAfterLogin() {
        val eas = LiteHostBridge.eas
        if (eas != null && !eas.isLogin()) {
            eas.ensureLogin(this, { refresh() }, {})
            return
        }
        refresh()
    }

    private fun refresh() {
        binding.refresh.isRefreshing = true
        viewModel.startRefresh()
    }

    private fun bindLiveData() {
        viewModel.examInfoLiveData.observe(this) { state ->
            binding.refresh.isRefreshing = false
            if (state.state == DataState.STATE.SUCCESS) {
                listAdapter.notifyItemChangedSmooth(state.data.orEmpty())
            }
            binding.emptyView.visibility =
                if (state.data.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
}
