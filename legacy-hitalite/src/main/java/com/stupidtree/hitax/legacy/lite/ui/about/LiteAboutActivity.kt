package com.stupidtree.hitax.legacy.lite.ui.about

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteAboutActivityBinding
import com.stupidtree.style.base.BaseActivity

/**
 * Lite 关于页。
 *
 * 布局沿用 HITA_Lite 的 activity_about：渐变头部、圆形图标、版本行、协议入口与检查更新按钮。
 * 版本号取自宿主包信息；协议正文为本地静态文本，不请求静态网页，也不依赖旧更新仓库。
 *
 * 契约：
 * - 前置条件：宿主已安装本应用，包信息可读取。
 * - 后置条件：版本行显示 `版本：<versionName>`；点击协议入口弹出 [LiteUserAgreementDialog]。
 * - 不变式：不创建 Room、Retrofit、登录单例或任何网络请求。
 * - 违约处理：包信息不可读时版本行只显示前缀，不崩溃。
 */
class LiteAboutActivity : BaseActivity<LiteAboutViewModel, LiteAboutActivityBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.liteAboutToolbar)
    }

    override fun initViewBinding(): LiteAboutActivityBinding =
        LiteAboutActivityBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteAboutViewModel> =
        LiteAboutViewModel::class.java

    override fun initViews() {
        binding.liteAboutAppName.text = applicationInfo.loadLabel(packageManager)
        binding.liteAboutVersion.text = getString(R.string.lite_about_version) + versionName()
        binding.liteAboutAboutInfo.text = ""
        binding.liteAboutPrivacyProtocol.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(TAG_AGREEMENT) == null) {
                LiteUserAgreementDialog().show(supportFragmentManager, TAG_AGREEMENT)
            }
        }
        binding.liteAboutCheck.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            Toast.makeText(
                this,
                getString(R.string.lite_about_local_build_notice, versionName()),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }

    private companion object {
        const val TAG_AGREEMENT = "lite_about_agreement"
    }
}
