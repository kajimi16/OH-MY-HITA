package com.stupidtree.hitax.legacy.lite.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.appbar.AppBarLayout
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteProfileActivityBinding
import com.stupidtree.hita.api.UserProfileSnapshot
import com.stupidtree.stupiduser.util.ImageUtils
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.widgets.PopUpText

/**
 * HITA_Lite 资料页。
 *
 * 布局与 Lite 的 activity_profile 一致；只读取宿主的当前会话快照。
 * TODO 宿主未提供按 id 查询他人资料、关注/粉丝计数与资料编辑契约，
 * 这些入口保留界面但提示不可用。
 */
class LiteProfileActivity :
    BaseActivity<LiteProfileViewModel, LiteProfileActivityBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViewBinding(): LiteProfileActivityBinding =
        LiteProfileActivityBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteProfileViewModel> =
        LiteProfileViewModel::class.java

    override fun initViews() {
        binding.toolbar.title = ""
        binding.fab.backgroundTintList = ColorStateList.valueOf(getColorPrimary())
        setUpAppBar()
        binding.refresh.setColorSchemeColors(getColorPrimary())
        binding.refresh.setOnRefreshListener { binding.refresh.isRefreshing = false }
        binding.logout.setOnClickListener { confirmLogout() }
        binding.followingLayout.visibility = View.GONE
        binding.fansLayout.visibility = View.GONE
        binding.postsLayout.visibility = View.GONE
        viewModel.profileLiveData.observe(this) { snapshot ->
            binding.refresh.isRefreshing = false
            render(snapshot)
        }
    }

    private fun render(snapshot: UserProfileSnapshot?) {
        binding.logout.visibility = View.GONE
        if (snapshot == null) {
            binding.textUsername.text = getString(R.string.lite_profile_not_logged_in)
            return
        }
        val requestedId = intent.getStringExtra(EXTRA_ID)
        if (!requestedId.isNullOrEmpty() && requestedId != snapshot.id) {
            Toast.makeText(this, R.string.lite_profile_lookup_unavailable, Toast.LENGTH_SHORT)
                .show()
            binding.textUsername.text = requestedId
            binding.fab.setText(R.string.lite_profile_follow)
            binding.fab.setIconResource(R.drawable.lite_profile_ic_person_add)
            binding.fab.isEnabled = true
            binding.fab.setOnClickListener {
                Toast.makeText(this, R.string.lite_profile_follow_unavailable, Toast.LENGTH_SHORT)
                    .show()
            }
            return
        }
        ImageUtils.loadAvatarInto(this, snapshot.avatar, binding.avatar)
        binding.textUsername.text = snapshot.username ?: "-"
        binding.textNickname.text = snapshot.nickname ?: "-"
        binding.textSignature.text = if (snapshot.signature.isNullOrEmpty()) {
            getString(R.string.lite_profile_no_signature)
        } else {
            snapshot.signature
        }
        // 快照没有性别字段，保留图标但隐藏，等宿主补齐资料契约后再显示。
        binding.iconGender.visibility = View.INVISIBLE
        binding.logout.visibility = View.VISIBLE
        binding.fab.setText(R.string.lite_profile_edit)
        binding.fab.setIconResource(R.drawable.lite_profile_ic_edit)
        binding.fab.isEnabled = true
        binding.fab.setOnClickListener {
            Toast.makeText(this, R.string.lite_profile_edit_unavailable, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun setUpAppBar() {
        val initElevation = binding.avatarCard.cardElevation
        binding.appbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                val percentage =
                    -verticalOffset.toFloat() / binding.appbar.totalScrollRange.toFloat()
                binding.avatarCard.pivotY = binding.avatarCard.height.toFloat() * 0.5f
                binding.avatarCard.pivotX = binding.avatarCard.width.toFloat() * 0.3f
                binding.avatarCard.scaleX = 1 - percentage
                binding.avatarCard.scaleY = 1 - percentage
                binding.textNickname.alpha = 1 - 2.5f * percentage
                binding.textSignature.alpha = 1 - 2.5f * percentage
                binding.iconGender.alpha = 1 - 2.5f * percentage
                binding.avatarCard.cardElevation = initElevation * (1 - percentage)
            }
        )
    }

    private fun confirmLogout() {
        PopUpText().setTitle(R.string.lite_profile_logout_hint).setOnConfirmListener(
            object : PopUpText.OnConfirmListener {
                override fun OnConfirm() {
                    viewModel.logout()
                    LiteHostBridge.timetable?.actionClearData()
                    LiteHostBridge.openLogin?.invoke(this@LiteProfileActivity)
                    finish()
                }
            }
        ).show(supportFragmentManager, "logout")
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
