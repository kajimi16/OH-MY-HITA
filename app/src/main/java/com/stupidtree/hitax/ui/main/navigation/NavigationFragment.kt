package com.stupidtree.hitax.ui.main.navigation

import com.stupidtree.hitax.HitaServices
import com.stupidtree.hitax.HApplication
import com.stupidtree.hita.api.EasService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import com.bumptech.glide.Glide
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.FragmentNavigationBinding
import com.stupidtree.style.base.BaseFragment
import com.stupidtree.hitax.utils.ActivityUtils.CourseResourceMode
import com.stupidtree.hitax.utils.ActivityUtils
import com.stupidtree.hitax.utils.ImageUtils
import com.stupidtree.stupiduser.data.repository.LocalUserRepository
import com.stupidtree.style.widgets.PopUpText
import com.stupidtree.hitax.data.source.preference.CourseReminderStore
import com.stupidtree.hitax.data.work.CourseReminderScheduler
import com.stupidtree.hitax.utils.IcsImportUtils
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class NavigationFragment : BaseFragment<NavigationViewModel, FragmentNavigationBinding>() {

    private fun eas(): EasService? =
        (requireContext().applicationContext as HApplication).pluginHost.service(EasService.KEY)

    
    // ICS 文件选择器
    private val selectIcsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { importICS(it) }
    }
    
    override fun getViewModelClass(): Class<NavigationViewModel> {
        return NavigationViewModel::class.java
    }

    override fun initViews(view: View) {
        viewModel.recentTimetableLiveData.observe(this) {
            if (it == null) {
                binding?.recentSubtitle?.setText(R.string.none)
            } else {
                binding?.recentSubtitle?.text = it.name
            }
        }
        viewModel.timetableCountLiveData.observe(this) {
            if (it == 0) {
                binding?.timetableSubtitle?.setText(R.string.no_timetable)
            } else {
                binding?.timetableSubtitle?.text = getString(R.string.timetable_count_format, it)
            }

        }
        binding?.cardTimetable?.setOnClickListener {
            ActivityUtils.startTimetableManager(requireContext())
        }
        binding?.cardRecentTimetable?.setOnClickListener {
            viewModel.recentTimetableLiveData.value?.let {
                ActivityUtils.startTimetableDetailActivity(requireContext(), it.id)
            }
        }
        binding?.cardImport?.setOnClickListener {
            eas()?.openImportTimetable(requireActivity())
        }
        binding?.cardEmptyClassroom?.setOnClickListener {
            eas()?.openClassroom(requireActivity())
        }
        binding?.cardScores?.setOnClickListener {
            eas()?.openScores(requireActivity())
        }
        binding?.cardSubjects?.setOnClickListener {
            eas()?.openExam(requireActivity())
        }
        binding?.cardCourseLookup?.setOnClickListener {
            ActivityUtils.startCourseResourceSearchActivity(requireContext(), mode = CourseResourceMode.VIEW)
        }
        binding?.cardCourseSubmitPr?.setOnClickListener {
            ActivityUtils.startCourseResourceSearchActivity(requireContext(), mode = CourseResourceMode.SUBMIT)
        }
        
        // 课程提醒开关
        val reminderStore = CourseReminderStore.getInstance(requireContext())
        binding?.switchCourseReminder?.isChecked = reminderStore.isEnabled()
        binding?.switchCourseReminder?.setOnCheckedChangeListener { _, isChecked ->
            reminderStore.setEnabled(isChecked)
            CourseReminderScheduler.autoSchedule(requireContext())
            val msg = if (isChecked) "课程提醒已开启" else "课程提醒已关闭"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
        binding?.cardCourseReminder?.setOnClickListener {
            binding?.switchCourseReminder?.toggle()
        }
        
        // ICS 导入
        binding?.cardIcsImport?.setOnClickListener {
            selectIcsLauncher.launch(IcsImportUtils.pickerMimeTypes())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        viewModel.startRefresh()

        LocalUserRepository.getInstance(requireContext()).getLoggedInUser().let {
            if (it.isValid()) { //如果已登录
                //装载头像
                binding?.avatar?.let { it1 ->
                    com.stupidtree.stupiduser.util.ImageUtils.loadAvatarInto(
                        requireContext(),
                        it.avatar,
                        it1
                    )
                }
                binding?.avatar?.let { it1 ->
                    com.stupidtree.stupiduser.util.ImageUtils.loadAvatarInto(
                        requireContext(),
                        it.avatar,
                        it1
                    )
                }
                //设置各种文字
                binding?.username?.text = it.username
                binding?.nickname?.text = it.nickname
                binding?.userCard?.setOnClickListener { _ ->
                    ActivityUtils.startProfileActivity(
                        requireContext(),
                        it.id,
                        binding?.avatar
                    )
                }
            } else {
                //未登录的信息显示
                val easService = eas()
                val easName = easService?.accountName()
                if (easName != null) {
                    binding?.username?.text = easName
                    binding?.nickname?.text = easService.accountSubtitle()
                } else {
                    binding?.username?.setText(R.string.eas_account_not_logged_in_title)
                    binding?.nickname?.setText(R.string.eas_account_not_logged_in_subtitle)
                }
                binding?.avatar?.setImageResource(R.drawable.place_holder_avatar)
                binding?.userCard?.setOnClickListener {
                    easService?.ensureLogin(requireActivity(), onSuccess = { refreshEasState() })
                }
            }
        }
        refreshEasState()
    }

    private fun refreshEasState() {
        val easService = eas() ?: return
        val name = easService.accountName()
        if (name != null) {
            binding?.easTitle?.text = "教务登录为 $name"
            binding?.easDot?.imageTintList = ColorStateList.valueOf(getColorPrimary())
            binding?.easAvatar?.let { ImageUtils.loadEASPictureInto(easService.accountAvatarUrl() ?: "", it) }
            binding?.easActionIcon?.setOnClickListener {
                PopUpText().setTitle(R.string.menu_logout_jw).setOnConfirmListener(object : PopUpText.OnConfirmListener {
                    override fun OnConfirm() {
                        easService.logout()
                        refreshEasState()
                    }
                }).show(parentFragmentManager, "logout")
            }
            binding?.easActionIcon?.visibility = View.VISIBLE
            binding?.easLayout?.isClickable = false
        } else {
            binding?.easTitle?.text = "未登录教务"
            binding?.easDot?.imageTintList = ColorStateList.valueOf(getColorPrimaryDisabled())
            binding?.easActionIcon?.visibility = View.INVISIBLE
            binding?.easLayout?.isClickable = true
            binding?.easLayout?.setOnClickListener {
                easService.ensureLogin(requireActivity(), onSuccess = { refreshEasState() })
            }
        }
    }

    override fun initViewBinding(): FragmentNavigationBinding {
        return FragmentNavigationBinding.inflate(layoutInflater)
    }
    
    /**
     * 导入 ICS 文件
     */
    private fun importICS(uri: Uri) {
        val context = requireContext()

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(context, "无法读取所选 ICS 文件", Toast.LENGTH_SHORT).show()
                return
            }
            val timetableRepo = HitaServices.timetable(context.applicationContext as android.app.Application)

            timetableRepo.importFromICSAsNewTimetable(
                inputStream,
                IcsImportUtils.getDisplayName(context, uri)
            )
                .observe(this) { result ->
                    when (result.state) {
                        com.stupidtree.component.data.DataState.STATE.SUCCESS -> {
                            val importResult = result.data ?: return@observe
                            Toast.makeText(
                                context,
                                "已创建课表“${importResult.timetableName}”，导入 ${importResult.importedCount} 个课程",
                                Toast.LENGTH_SHORT
                            ).show()
                            ActivityUtils.startTimetableDetailActivity(
                                context,
                                importResult.timetableId
                            )
                        }
                        com.stupidtree.component.data.DataState.STATE.FETCH_FAILED -> {
                            Toast.makeText(context, "导入失败: ${result.message}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(context, "读取文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
