package com.stupidtree.hitax.legacy.lite.ui.timetable.manager

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteActivityTimetableManagerBinding
import com.stupidtree.hitax.utils.EditModeHelper
import com.stupidtree.hitax.utils.ImageUtils
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter

/**
 * HITA_Lite 课表管理页。
 *
 * 布局与 Lite 2.1 的 activity_timetable_manager 一致；增删与详情走宿主
 * TimetableService / TimetableUiService，不复制 Lite 旧 Repository。
 */
class LiteTimetableManagerActivity :
    BaseActivity<LiteTimetableManagerViewModel, LiteActivityTimetableManagerBinding>(),
    EditModeHelper.EditableContainer<Timetable> {

    private lateinit var listAdapter: LiteTimetableListAdapter
    private var editModeHelper: EditModeHelper<Timetable>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViewBinding(): LiteActivityTimetableManagerBinding =
        LiteActivityTimetableManagerBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteTimetableManagerViewModel> =
        LiteTimetableManagerViewModel::class.java

    override fun initViews() {
        binding.toolbar.title = ""
        binding.collapse.title = ""
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scale = 1.0f + verticalOffset / appBarLayout.height.toFloat()
            binding.title.translationX =
                (binding.toolbar.contentInsetStartWithNavigation + ImageUtils.dp2px(this, 8f)) *
                    (1 - scale)
            binding.title.scaleX = 0.5f * (1 + scale)
            binding.title.scaleY = 0.5f * (1 + scale)
            binding.title.translationY = (binding.title.height / 2) * (1 - binding.title.scaleY)
            binding.buttonSync.translationY = ImageUtils.dp2px(this, 24f) * (1 - scale)
            binding.buttonSync.scaleX = 0.7f + 0.3f * scale
            binding.buttonSync.scaleY = 0.7f + 0.3f * scale
            binding.buttonSync.translationX =
                (binding.buttonSync.width / 2) * (1 - binding.buttonSync.scaleX)
        }
        listAdapter = LiteTimetableListAdapter(this, mutableListOf())
        editModeHelper = EditModeHelper(this, listAdapter, this).apply {
            init(this@LiteTimetableManagerActivity, R.id.edit_layout)
            smoothSwitch = false
        }
        binding.list.adapter = listAdapter
        binding.list.layoutManager = GridLayoutManager(this, 2)
        listAdapter.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<Timetable> {
            override fun onItemClick(data: Timetable?, card: View?, position: Int) {
                if (data == null) {
                    viewModel.startNewTimetable()
                } else {
                    LiteHostBridge.openTimetableDetail?.invoke(this@LiteTimetableManagerActivity, data.id)
                }
            }
        })
        listAdapter.setOnAddClickListener(object : LiteTimetableListAdapter.OnAddClickListener {
            override fun onAddClick(source: LiteTimetableListAdapter.SOURCE) {
                if (source == LiteTimetableListAdapter.SOURCE.EAS) {
                    LiteHostBridge.easUi?.openImportTimetable(this@LiteTimetableManagerActivity)
                } else {
                    viewModel.startNewTimetable()
                }
            }
        })
        listAdapter.setOnItemLongClickListener(object :
            BaseListAdapter.OnItemLongClickListener<Timetable> {
            override fun onItemLongClick(data: Timetable?, view: View?, position: Int): Boolean {
                editModeHelper?.activateEditMode(position)
                return true
            }
        })
        binding.buttonSync.setOnClickListener {
            // 宿主没有 Lite 的私有同步后端；保留按钮外观，点击不触发第二套同步存储。
        }
        viewModel.timetablesLiveData.observe(this) {
            listAdapter.notifyDatasetChanged(it)
            binding.list.scheduleLayoutAnimation()
        }
    }

    override fun onEditClosed() = Unit

    override fun onEditStarted() = Unit

    override fun onItemCheckedChanged(position: Int, checked: Boolean, currentSelected: Int) = Unit

    override fun onDelete(toDelete: Collection<Timetable>?) {
        viewModel.startDeleteTimetables(toDelete?.toList().orEmpty())
        editModeHelper?.closeEditMode()
    }

    override fun onBackPressed() {
        if (editModeHelper?.isEditMode == true) {
            editModeHelper?.closeEditMode()
            return
        }
        super.onBackPressed()
    }
}
