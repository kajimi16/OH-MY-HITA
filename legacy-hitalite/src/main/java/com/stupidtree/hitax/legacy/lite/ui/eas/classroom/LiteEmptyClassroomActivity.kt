package com.stupidtree.hitax.legacy.lite.ui.eas.classroom

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ClassroomBuilding
import com.stupidtree.hita.api.ClassroomRoom
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteRoomActivityClassroomBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseListAdapter
import com.stupidtree.style.widgets.PopUpCheckableList

/**
 * HITA_Lite 空教室查询页。
 *
 * 教学楼与教室数据走宿主 EmptyClassroomService，学期由宿主服务按当前学期选定；
 * 未登录时先走宿主登录弹窗，不在本模块建第二套会话。
 */
class LiteEmptyClassroomActivity :
    BaseActivity<LiteEmptyClassroomViewModel, LiteRoomActivityClassroomBinding>() {

    private lateinit var listAdapter: LiteEmptyClassroomListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViewBinding(): LiteRoomActivityClassroomBinding =
        LiteRoomActivityClassroomBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteEmptyClassroomViewModel> =
        LiteEmptyClassroomViewModel::class.java

    override fun initViews() {
        binding.refresh.setColorSchemeColors(getColorPrimary())
        listAdapter = LiteEmptyClassroomListAdapter(this, mutableListOf()) {
            viewModel.structure()
        }
        binding.list.adapter = listAdapter
        binding.list.layoutManager = GridLayoutManager(this, 2)
        binding.refresh.setOnRefreshListener { viewModel.startRefresh() }
        binding.buildingLayout.setOnClickListener { showBuildingPicker() }
        binding.weekLayout.setOnClickListener { showWeekPicker() }
        listAdapter.setOnItemClickListener(object :
            BaseListAdapter.OnItemClickListener<ClassroomRoom> {
            override fun onItemClick(data: ClassroomRoom?, card: View?, position: Int) {
                val room = data ?: return
                LiteEmptyClassroomDetailFragment.newInstance(
                    room,
                    viewModel.timetableName(),
                    viewModel.selectedWeekLiveData.value ?: 1,
                    viewModel.structure()
                ).show(supportFragmentManager, "lite_room_detail")
            }
        })
        bindLiveData()
        startLoginThenLoad()
    }

    private fun startLoginThenLoad() {
        val account = LiteHostBridge.easAccount
        val ui = LiteHostBridge.easUi
        if (account == null || ui == null) {
            binding.refresh.isRefreshing = true
            viewModel.startRefresh()
            return
        }
        if (account.account().loggedIn) {
            binding.refresh.isRefreshing = true
            viewModel.startRefresh()
            return
        }
        ui.ensureLogin(this) { result ->
            if (result.success) {
                binding.refresh.isRefreshing = true
                viewModel.startRefresh()
            } else {
                finish()
            }
        }
    }

    private fun bindLiveData() {
        viewModel.buildingsLiveData.observe(this) { data ->
            if (data.state == DataState.STATE.SUCCESS && !data.data.isNullOrEmpty()) {
                if (viewModel.selectedBuildingLiveData.value == null) {
                    viewModel.selectBuilding(data.data!!.first())
                }
            } else {
                binding.refresh.isRefreshing = false
                binding.buildingText.setText(R.string.load_failed)
            }
        }
        viewModel.selectedBuildingLiveData.observe(this) { building ->
            building ?: return@observe
            binding.buildingText.text = building.name
            binding.refresh.isRefreshing = true
        }
        viewModel.currentWeekLiveData.observe(this) { defaultWeek ->
            if (viewModel.selectedWeekLiveData.value == null) {
                viewModel.selectWeek(defaultWeek)
            }
        }
        viewModel.selectedWeekLiveData.observe(this) { week ->
            binding.weekText.text = getString(R.string.week_title, week)
            binding.refresh.isRefreshing = true
        }
        viewModel.roomsLiveData.observe(this) { data ->
            binding.refresh.isRefreshing = false
            when (data.state) {
                DataState.STATE.SUCCESS -> listAdapter.notifyItemChangedSmooth(
                    data.data.orEmpty()
                )

                DataState.STATE.NOTHING -> Unit
                else -> listAdapter.notifyItemChangedSmooth(emptyList())
            }
        }
    }

    private fun showBuildingPicker() {
        val buildings = viewModel.buildingsLiveData.value?.data
        if (buildings.isNullOrEmpty()) return
        val names = buildings.map { it.name }
        PopUpCheckableList<ClassroomBuilding>()
            .setListData(names, buildings)
            .setTitle(getString(R.string.pick_query_building))
            .setOnConfirmListener(object :
                PopUpCheckableList.OnConfirmListener<ClassroomBuilding> {
                override fun OnConfirm(title: String?, key: ClassroomBuilding) {
                    viewModel.selectBuilding(key)
                }
            }).show(supportFragmentManager, "lite_room_building")
    }

    private fun showWeekPicker() {
        val weeks = (1..20).toList()
        PopUpCheckableList<Int>()
            .setListData(weeks.map { it.toString() }, weeks)
            .setTitle(getString(R.string.pick_query_week))
            .setOnConfirmListener(object : PopUpCheckableList.OnConfirmListener<Int> {
                override fun OnConfirm(title: String?, key: Int) {
                    viewModel.selectWeek(key)
                }
            }).show(supportFragmentManager, "lite_room_week")
    }
}
