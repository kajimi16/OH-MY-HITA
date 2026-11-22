package com.stupidtree.hitax.legacy.lite.ui.eas.score

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ScoreEntry
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteScoreActivityInquiryBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.widgets.PopUpCheckableList

/**
 * HITA_Lite 成绩查询页。
 *
 * 布局与 Lite 2.1 的成绩查询页一致；学期与成绩全部来自宿主
 * [com.stupidtree.hita.api.ScoreService]，不复制旧数据访问层。
 * 前置条件：宿主已注入 ScoreService 且已登录；未登录时提示登录并重试。
 * 后置条件：列表内容只反映服务返回的 ScoreReport.entries。
 */
class LiteScoreInquiryActivity :
    BaseActivity<LiteScoreInquiryViewModel, LiteScoreActivityInquiryBinding>() {

    private lateinit var listAdapter: LiteScoresListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
        binding.collapse.title = getString(R.string.lite_score_label)
    }

    override fun initViewBinding(): LiteScoreActivityInquiryBinding =
        LiteScoreActivityInquiryBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteScoreInquiryViewModel> =
        LiteScoreInquiryViewModel::class.java

    override fun initViews() {
        bindLiveData()
        binding.refresh.setColorSchemeColors(getColorPrimary())
        binding.refresh.setOnRefreshListener { refresh() }
        listAdapter = LiteScoresListAdapter(this, mutableListOf())
        binding.scoreStructure.adapter = listAdapter
        binding.scoreStructure.layoutManager = LinearLayoutManager(this)
        binding.schoolSemesterLayout.setOnClickListener { showTermPicker() }
        binding.testTypeLayout.setOnClickListener { showTypePicker() }
        listAdapter.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<ScoreEntry> {
            override fun onItemClick(
                data: ScoreEntry?,
                card: View?,
                position: Int
            ) {
                data?.let {
                    LiteScoreDetailFragment.newInstance(it)
                        .show(supportFragmentManager, "score_detail")
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
        viewModel.termsLiveData.observe(this) { data ->
            if (data.state == DataState.STATE.SUCCESS) {
                val terms = data.data
                if (terms.isNullOrEmpty()) {
                    binding.refresh.isRefreshing = false
                    binding.schoolSemesterText.setText(R.string.lite_score_no_term)
                } else {
                    val selected = viewModel.selectedTermLiveData.value
                    val target = terms.firstOrNull { it == selected }
                        ?: terms.firstOrNull { it.isCurrent }
                        ?: terms.first()
                    viewModel.selectedTermLiveData.value = target
                }
            } else {
                binding.refresh.isRefreshing = false
                binding.schoolSemesterText.setText(R.string.lite_score_load_failed)
            }
        }
        viewModel.selectedTermLiveData.observe(this) { term ->
            term ?: return@observe
            binding.schoolSemesterText.text = term.name
            binding.refresh.isRefreshing = true
        }
        viewModel.reportLiveData.observe(this) { state ->
            binding.refresh.isRefreshing = false
            if (state.state == DataState.STATE.SUCCESS) {
                listAdapter.notifyItemChangedSmooth(state.data?.entries.orEmpty())
            }
            binding.emptyView.visibility =
                if (state.data?.entries.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.includeResitLiveData.observe(this) { includeResit ->
            binding.refresh.isRefreshing = viewModel.selectedTermLiveData.value != null
            binding.testTypeText.setText(
                if (includeResit) R.string.lite_score_type_all else R.string.lite_score_type_normal
            )
        }
    }

    private fun showTermPicker() {
        val terms = viewModel.termsLiveData.value?.data.orEmpty()
        if (terms.isEmpty()) return
        PopUpCheckableList<TermItem>()
            .setListData(terms.map { it.name }, terms)
            .setTitle(getString(R.string.lite_score_pick_term))
            .setOnConfirmListener(object : PopUpCheckableList.OnConfirmListener<TermItem> {
                override fun OnConfirm(title: String?, key: TermItem) {
                    viewModel.selectedTermLiveData.value = key
                }
            })
            .show(supportFragmentManager, "terms")
    }

    private fun showTypePicker() {
        val names = listOf(
            getString(R.string.lite_score_type_all),
            getString(R.string.lite_score_type_normal)
        )
        val values = listOf(true, false)
        PopUpCheckableList<Boolean>()
            .setListData(names, values)
            .setTitle(getString(R.string.lite_score_pick_type))
            .setOnConfirmListener(object : PopUpCheckableList.OnConfirmListener<Boolean> {
                override fun OnConfirm(title: String?, key: Boolean) {
                    viewModel.includeResitLiveData.value = key
                }
            })
            .show(supportFragmentManager, "types")
    }
}
