package com.stupidtree.hitax.legacy.lite.ui.eas.imp

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteImportActivityImportBinding
import com.stupidtree.hitax.ui.widgets.PopUpCalendarPicker
import com.stupidtree.hitax.ui.widgets.PopUpTimePeriodPicker
import com.stupidtree.hitax.utils.AnimationUtils
import com.stupidtree.hitax.utils.ImageUtils.dp2px
import com.stupidtree.hitax.utils.TextTools
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter

/**
 * HITA_Lite 导入课表页。
 *
 * 页面展示当前课表的开始时间与课时结构，允许修改；导入动作交给宿主 EasService，
 * 登录、会话、抓取与 Room 写入都留在宿主插件内。
 */
class LiteImportTimetableActivity :
    BaseActivity<LiteImportTimetableViewModel, LiteImportActivityImportBinding>() {

    private lateinit var structureAdapter: LiteImportStructureListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViewBinding(): LiteImportActivityImportBinding =
        LiteImportActivityImportBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteImportTimetableViewModel> =
        LiteImportTimetableViewModel::class.java

    override fun initViews() {
        binding.toolbar.title = ""
        binding.collapse.title = ""
        binding.appbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val scale = 1.0f + verticalOffset / appBarLayout.height.toFloat()
                binding.timetableText.translationX =
                    (binding.toolbar.contentInsetStartWithNavigation + dp2px(this, 8f)) *
                        (1 - scale)
                binding.timetableText.scaleX = 0.5f * (1 + scale)
                binding.timetableText.scaleY = 0.5f * (1 + scale)
                binding.timetableText.translationY =
                    (binding.timetableText.height / 2) * (1 - binding.timetableText.scaleY)
                binding.buttonImport.translationY = dp2px(this, 24f) * (1 - scale)
                binding.buttonImport.scaleX = 0.7f + 0.3f * scale
                binding.buttonImport.scaleY = 0.7f + 0.3f * scale
                binding.buttonImport.translationX =
                    (binding.buttonImport.width / 2) * (1 - binding.buttonImport.scaleX)
            })
        binding.cardName.isEnabled = false
        binding.buttonImport.setOnClickListener {
            val ui = LiteHostBridge.easUi ?: return@setOnClickListener
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            ui.openImportTimetable(this)
        }
        binding.cardDate.onCardClickListener = View.OnClickListener {
            PopUpCalendarPicker()
                .setInitValue(viewModel.startDate().timeInMillis)
                .setOnConfirmListener(object : PopUpCalendarPicker.OnConfirmListener {
                    override fun onConfirm(c: java.util.Calendar) {
                        viewModel.changeStartDate(c)
                        refreshCards()
                    }
                }).show(supportFragmentManager, "lite_import_date")
        }
        initList()
        bindLiveData()
    }

    private fun initList() {
        structureAdapter = LiteImportStructureListAdapter(this, mutableListOf())
        binding.scheduleStructure.adapter = structureAdapter
        binding.scheduleStructure.layoutManager = LinearLayoutManager(this)
        binding.refresh.setColorSchemeColors(getColorPrimary())
        binding.refresh.setOnRefreshListener {
            structureAdapter.notifyDataSetChanged(viewModel.currentStructure())
            binding.refresh.isRefreshing = false
        }
        structureAdapter.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<TimePeriodInDay> {
            override fun onItemClick(data: TimePeriodInDay?, card: View?, position: Int) {
                val period = data ?: return
                PopUpTimePeriodPicker()
                    .setInitialValue(period.from, period.to)
                    .setDialogTitle(R.string.pick_time_period)
                    .setOnDialogConformListener(object :
                        PopUpTimePeriodPicker.OnDialogConformListener {
                        override fun onClick(timePeriodInDay: TimePeriodInDay) {
                            viewModel.setStructureData(timePeriodInDay, position)
                            structureAdapter.notifyDataSetChanged(viewModel.currentStructure())
                        }
                    }).show(supportFragmentManager, "lite_import_period")
            }
        })
    }

    private fun bindLiveData() {
        viewModel.timetableLiveData.observe(this) { timetable ->
            binding.refresh.isRefreshing = false
            binding.timetableText.text =
                timetable?.name ?: getString(R.string.lite_import_no_timetable)
            binding.cardName.setTitle(
                timetable?.name ?: getString(R.string.lite_import_no_timetable)
            )
            structureAdapter.notifyDataSetChanged(timetable?.scheduleStructure.orEmpty())
            refreshCards()
            AnimationUtils.enableLoadingButton(
                binding.buttonImport,
                timetable != null && viewModel.hasEas()
            )
        }
    }

    private fun refreshCards() {
        binding.cardDate.setTitle(
            if (viewModel.timetableLiveData.value == null) {
                getString(R.string.lite_import_no_date)
            } else {
                TextTools.getNormalDateText(this, viewModel.startDate())
            }
        )
    }
}
