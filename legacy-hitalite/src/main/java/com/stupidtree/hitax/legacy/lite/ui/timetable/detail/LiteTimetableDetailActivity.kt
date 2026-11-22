package com.stupidtree.hitax.legacy.lite.ui.timetable.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteDetailActivityBinding
import com.stupidtree.hitax.legacy.lite.ui.eas.imp.LiteImportStructureListAdapter
import com.stupidtree.hitax.legacy.lite.ui.event.add.LiteAddEventDialog
import com.stupidtree.hitax.ui.widgets.PopUpCalendarPicker
import com.stupidtree.hitax.ui.widgets.PopUpTimePeriodPicker
import com.stupidtree.hitax.utils.EditModeHelper
import com.stupidtree.hitax.utils.ImageUtils
import com.stupidtree.hitax.utils.TextTools
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.widgets.PopUpColorPicker
import com.stupidtree.style.widgets.PopUpEditText
import com.stupidtree.style.widgets.PopUpText
import java.util.Calendar

/**
 * HITA_Lite 课表详情页。
 *
 * 前置条件：宿主已通过 PresetShellRouter 注入 TimetableService 与 TimetableUiService。
 * 后置条件：页面展示课表信息、科目、教师与课时结构；写操作全部交给宿主服务。
 * 不变式：不创建 Room、Retrofit、Repository 或登录单例。
 * 违约处理：服务缺失时页面仍可打开，点击动作不写入。
 */
class LiteTimetableDetailActivity :
    BaseActivity<LiteTimetableDetailViewModel, LiteDetailActivityBinding>() {

    private var subjectsAdapter: LiteSubjectsListAdapter? = null
    private var teachersListAdapter: LiteTeachersListAdapter? = null
    private lateinit var scheduleStructureAdapter: LiteImportStructureListAdapter
    private var editModeHelper: EditModeHelper<TermSubject>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }
    override fun initViews() {
        bindLiveData()
        binding.usercenterSubjectsList.setItemViewCacheSize(20)
        subjectsAdapter = LiteSubjectsListAdapter(this, mutableListOf(), viewModel, this)
        binding.usercenterSubjectsList.layoutManager = LinearLayoutManager(this)
        binding.usercenterSubjectsList.adapter = subjectsAdapter
        teachersListAdapter = LiteTeachersListAdapter(this, mutableListOf())
        binding.teachersList.adapter = teachersListAdapter
        binding.teachersList.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.toolbar.title = ""
        binding.collapse.title = ""
        teachersListAdapter?.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<TeacherInfo> {
            override fun onItemClick(data: TeacherInfo?, card: View?, position: Int) {
                LiteHostBridge.resourceUi?.openSearch(this@LiteTimetableDetailActivity, data?.name, true)
            }
        })
        subjectsAdapter?.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<TermSubject> {
            override fun onItemClick(data: TermSubject?, card: View?, position: Int) {
                data?.let { LiteHostBridge.timetableUi?.openSubject(this@LiteTimetableDetailActivity, it.id) }
            }
        })
        subjectsAdapter?.setOnItemLongClickListener(object :
            BaseListAdapter.OnItemLongClickListener<TermSubject> {
            override fun onItemLongClick(
                data: TermSubject?,
                view: View?,
                position: Int,
            ): Boolean {
                if (data?.type == TermSubject.TYPE.TAG) return false
                editModeHelper?.activateEditMode(position)
                return true
            }
        })
        subjectsAdapter?.onColorClickListener = object : LiteSubjectsListAdapter.OnColorClickListener {
            override fun onColorClick(data: TermSubject) {
                PopUpColorPicker()
                    .setOnColorSelectListener(object : PopUpColorPicker.OnColorSelectedListener {
                        override fun onSelected(color: Int) {
                            viewModel.startChangeSubjectColor(data.id, color)
                        }
                    })
                    .initColor(data.color)
                    .show(supportFragmentManager, "lite_detail_pick_color")
            }
        }
        subjectsAdapter?.let { adapter ->
            editModeHelper = EditModeHelper(
                this,
                adapter,
                object : EditModeHelper.EditableContainer<TermSubject> {
                    override fun onEditClosed() {
                        binding.subjectLabel.visibility = View.VISIBLE
                    }

                    override fun onEditStarted() {
                        binding.subjectLabel.visibility = View.GONE
                    }

                    override fun onItemCheckedChanged(
                        position: Int,
                        checked: Boolean,
                        currentSelected: Int,
                    ) = Unit

                    override fun onDelete(toDelete: Collection<TermSubject>?) {
                        editModeHelper?.closeEditMode()
                        toDelete?.let { viewModel.startDeleteSubjects(it) }
                    }
                },
            )
        }
        editModeHelper?.init(this, R.id.edit_layout)
        editModeHelper?.smoothSwitch = true
        scheduleStructureAdapter = LiteImportStructureListAdapter(this, mutableListOf())
        binding.scheduleStructure.adapter = scheduleStructureAdapter
        binding.scheduleStructure.layoutManager = LinearLayoutManager(this)
        scheduleStructureAdapter.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<TimePeriodInDay> {
            override fun onItemClick(data: TimePeriodInDay?, card: View?, position: Int) {
                val period = data ?: return
                PopUpTimePeriodPicker()
                    .setInitialValue(period.from, period.to)
                    .setDialogTitle(R.string.lite_detail_pick_time_period)
                    .setOnDialogConformListener(object :
                        PopUpTimePeriodPicker.OnDialogConformListener {
                        override fun onClick(timePeriodInDay: TimePeriodInDay) {
                            viewModel.startChangeTimetableStructure(timePeriodInDay, position)
                        }
                    })
                    .show(supportFragmentManager, "lite_detail_pick_period")
            }
        })
        binding.appbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val scale = 1.0f + verticalOffset / appBarLayout.height.toFloat()
                binding.title.translationX =
                    (binding.toolbar.contentInsetStartWithNavigation +
                        ImageUtils.dp2px(this, 8f)) * (1 - scale)
                binding.title.scaleX = 0.5f * (1 + scale)
                binding.title.scaleY = 0.5f * (1 + scale)
                binding.title.translationY =
                    (binding.title.height / 2) * (1 - binding.title.scaleY)
                binding.share.translationY = ImageUtils.dp2px(this, 24f) * (1 - scale)
                binding.share.scaleX = 0.7f + 0.3f * scale
                binding.share.scaleY = 0.7f + 0.3f * scale
                binding.share.translationX =
                    (binding.share.width / 2) * (1 - binding.share.scaleX)
            }
        )
        binding.cardDate.onCardClickListener = View.OnClickListener {
            PopUpCalendarPicker()
                .setInitValue(viewModel.timetableLiveData.value?.startTime?.time)
                .setOnConfirmListener(object : PopUpCalendarPicker.OnConfirmListener {
                    override fun onConfirm(c: Calendar) {
                        viewModel.startChangeTimetableStartTime(c.timeInMillis)
                    }
                })
                .show(supportFragmentManager, "lite_detail_pick_date")
        }
        binding.cardName.onCardClickListener = View.OnClickListener {
            viewModel.timetableLiveData.value?.let { timetable ->
                PopUpEditText()
                    .setTitle(R.string.lite_detail_set_timetable_name)
                    .setText(timetable.name)
                    .setOnConfirmListener(object : PopUpEditText.OnConfirmListener {
                        override fun OnConfirm(text: String) {
                            if (text.isBlank()) return
                            timetable.name = text
                            viewModel.startSaveTimetableInfo()
                        }
                    })
                    .show(supportFragmentManager, "lite_detail_set_name")
            }
        }
        binding.reset.setOnClickListener {
            PopUpText()
                .setTitle(R.string.lite_detail_random_allocate)
                .setOnConfirmListener(object : PopUpText.OnConfirmListener {
                    override fun OnConfirm() {
                        viewModel.startResetSubjectColors()
                    }
                })
                .show(supportFragmentManager, "lite_detail_reset_colors")
        }
        binding.share.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            binding.share.startAnimation()
            viewModel.exportToIcs()
        }
        binding.add.setOnClickListener {
            LiteAddEventDialog.newInstance(true)
                .setInitTimetable(viewModel.timetableLiveData.value)
                .show(supportFragmentManager, "lite_detail_add_event")
        }
    }
    private fun bindLiveData() {
        viewModel.timetableLiveData.observe(this) { timetable ->
            binding.title.text = timetable.name
            binding.cardName.setTitle(timetable.name)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timetable.startTime.time
            binding.cardDate.setTitle(TextTools.getNormalDateText(this, calendar))
            scheduleStructureAdapter.notifyItemChangedSmooth(timetable.scheduleStructure)
        }
        viewModel.subjectsLiveData.observe(this) { subjects ->
            if (subjectsAdapter?.beans.isNullOrEmpty()) {
                subjectsAdapter?.notifyDataSetChanged(subjects)
                binding.usercenterSubjectsList.scheduleLayoutAnimation()
            } else {
                subjectsAdapter?.notifyItemChangedSmooth(
                    subjects,
                    object : BaseListAdapter.RefreshJudge<TermSubject> {
                        override fun judge(oldData: TermSubject, newData: TermSubject): Boolean =
                            oldData.name != newData.name || oldData.color != newData.color
                    },
                    Comparator { first, second -> first.name.compareTo(second.name) },
                )
            }
        }
        viewModel.teacherInfoLiveData.observe(this) { teachers ->
            val visibleTeachers = teachers.filter { !it.name.isNullOrEmpty() }.toMutableList()
            if (teachersListAdapter?.beans.isNullOrEmpty()) {
                teachersListAdapter?.notifyDataSetChanged(visibleTeachers)
                binding.teachersList.scheduleLayoutAnimation()
            } else {
                teachersListAdapter?.notifyItemChangedSmooth(visibleTeachers)
            }
        }
        viewModel.exportToICSResult.observe(this) { result ->
            val success = result.state == DataState.STATE.SUCCESS
            binding.share.performHapticFeedback(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
            )
            val bitmap = ImageUtils.getResourceBitmap(
                this,
                if (success) {
                    com.stupidtree.style.R.drawable.ic_baseline_done_24
                } else {
                    com.stupidtree.style.R.drawable.ic_baseline_error_24
                },
            )
            binding.share.doneLoadingAnimation(getColorPrimary(), bitmap)
            binding.share.postDelayed({ binding.share.revertAnimation() }, 600)
            if (success) {
                Toast.makeText(this, R.string.lite_detail_export_success, Toast.LENGTH_SHORT)
                    .show()
                val imageIntent = Intent(Intent.ACTION_SEND)
                imageIntent.type = "application/octet-stream"
                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(result.data))
                startActivity(
                    Intent.createChooser(imageIntent, getString(R.string.lite_detail_share))
                )
            } else {
                Toast.makeText(this, R.string.lite_detail_export_failed, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        intent.getStringExtra(EXTRA_TIMETABLE_ID)?.let { viewModel.startRefresh(it) }
    }

    override fun initViewBinding(): LiteDetailActivityBinding =
        LiteDetailActivityBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteTimetableDetailViewModel> =
        LiteTimetableDetailViewModel::class.java

    companion object {
        const val EXTRA_TIMETABLE_ID = "id"
    }
}
