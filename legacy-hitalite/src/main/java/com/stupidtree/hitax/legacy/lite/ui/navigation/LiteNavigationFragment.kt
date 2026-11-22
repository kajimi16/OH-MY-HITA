package com.stupidtree.hitax.legacy.lite.ui.navigation

import android.view.View
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteFragmentNavigationBinding
import com.stupidtree.style.base.BaseFragment

/**
 * HITA_Lite 功能中心。
 *
 * 视觉按 Lite 2.1 的 fragment_navigation.xml；数据与登录交给宿主服务。
 * 前置条件：`LiteHostBridge` 已注入；缺失时页面保持空态且入口为空操作。
 * 后置条件：不创建 Lite 旧 Room、旧 EAS 或旧登录 Activity。
 */
class LiteNavigationFragment :
    BaseFragment<LiteNavigationViewModel, LiteFragmentNavigationBinding>() {

    override fun getViewModelClass(): Class<LiteNavigationViewModel> =
        LiteNavigationViewModel::class.java

    override fun initViewBinding(): LiteFragmentNavigationBinding =
        LiteFragmentNavigationBinding.inflate(layoutInflater)

    override fun initViews(view: View) {
        viewModel.recentTimetableLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                binding?.recentSubtitle?.setText(R.string.lite_nav_none)
            } else {
                binding?.recentSubtitle?.text = it.name
            }
        }
        viewModel.timetableCountLiveData.observe(viewLifecycleOwner) {
            if (it == 0) {
                binding?.timetableSubtitle?.setText(R.string.lite_nav_no_timetable)
            } else {
                binding?.timetableSubtitle?.text =
                    getString(R.string.lite_nav_timetable_count_format, it)
            }
        }
        viewModel.unreadMessageLiveData.observe(viewLifecycleOwner) {
            binding?.messageNum?.visibility = View.GONE
        }
        binding?.cardTimetable?.setOnClickListener {
            LiteHostBridge.openTimetableManager?.invoke(requireActivity())
        }
        binding?.cardRecentTimetable?.setOnClickListener {
            viewModel.recentTimetableLiveData.value?.let {
                LiteHostBridge.openTimetableDetail?.invoke(requireActivity(), it.id)
            }
        }
        binding?.cardImport?.setOnClickListener {
            LiteHostBridge.openImport?.invoke(requireActivity())
        }
        binding?.cardEmptyClassroom?.setOnClickListener {
            LiteHostBridge.openClassroom?.invoke(requireActivity())
        }
        binding?.cardScores?.setOnClickListener {
            LiteHostBridge.openScores?.invoke(requireActivity())
        }
        binding?.cardSubjects?.setOnClickListener {
            LiteHostBridge.openExams?.invoke(requireActivity())
        }
        binding?.search?.setOnClickListener {
            LiteHostBridge.openSearch?.invoke(requireActivity())
        }
    }

    override fun onStart() {
        super.onStart()
        refreshAccountEntry()
    }

    override fun onResume() {
        super.onResume()
        refreshAccountEntry()
    }

    private fun refreshAccountEntry() {
        val eas = LiteHostBridge.eas
        if (eas?.isLogin() == true) {
            val accountName = eas.accountSubtitle()?.takeIf { it.isNotBlank() }
                ?: eas.accountName()
            binding?.nickname?.setText(R.string.lite_nav_jw_login_connected_title)
            binding?.username?.text = accountName?.let {
                getString(R.string.lite_nav_jw_login_status_with_student_id, it)
            }
            binding?.avatar?.setImageResource(R.drawable.lite_place_holder_avatar)
            binding?.userStatusDot?.visibility = View.VISIBLE
            binding?.userActionIcon?.setOnClickListener {
                eas.logout()
                refreshAccountEntry()
            }
            binding?.userCard?.setOnClickListener(null)
        } else {
            binding?.nickname?.setText(R.string.lite_nav_jw_login_entry_title)
            binding?.username?.setText(R.string.lite_nav_jw_login_entry_subtitle)
            binding?.avatar?.setImageResource(R.drawable.lite_place_holder_avatar)
            binding?.userStatusDot?.visibility = View.GONE
            binding?.userActionIcon?.setOnClickListener {
                openAccount()
            }
            binding?.userCard?.setOnClickListener {
                openAccount()
            }
        }
    }

    private fun openAccount() {
        val activity = requireActivity()
        if (LiteHostBridge.easAuth != null) {
            if (parentFragmentManager.findFragmentByTag(TAG_EAS_LOGIN) == null) {
                LiteEasLoginDialog().show(parentFragmentManager, TAG_EAS_LOGIN)
            }
        } else {
            val eas = LiteHostBridge.eas
            if (eas != null) {
                eas.openAccount(activity)
            } else {
                android.widget.Toast.makeText(
                    activity,
                    R.string.lite_login_unavailable,
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private companion object {
        const val TAG_EAS_LOGIN = "lite_eas_login"
    }
}
