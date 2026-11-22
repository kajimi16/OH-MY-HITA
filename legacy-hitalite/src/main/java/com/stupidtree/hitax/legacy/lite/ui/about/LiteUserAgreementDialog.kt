package com.stupidtree.hitax.legacy.lite.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteAboutDialogUserAgreementBinding
import com.stupidtree.hitax.legacy.lite.databinding.LiteAboutDynamicUserAgreementBinding

/**
 * 用户协议与隐私协议底部弹窗。
 *
 * 两个页签的正文来自本地字符串资源，不请求静态网页，也不需要登录态。
 * 默认构造函数可直接由宿主抽屉的 `LiteHostBridge.openAgreement` 使用。
 *
 * 契约：
 * - 前置条件：宿主 FragmentManager 处于活跃状态。
 * - 后置条件：显示「用户协议」「隐私协议」两个可滑动页签。
 * - 不变式：不发起网络请求；视图销毁后不持有 Binding。
 * - 违约处理：无。
 */
class LiteUserAgreementDialog : BottomSheetDialogFragment() {

    private var binding: LiteAboutDialogUserAgreementBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.stupidtree.style.R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val created = LiteAboutDialogUserAgreementBinding.inflate(inflater, container, false)
        binding = created
        return created.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val root = binding ?: return
        val pages = listOf(
            getString(R.string.lite_about_user_agreement_text),
            getString(R.string.lite_about_privacy_agreement_text),
        )
        root.liteAboutPager.adapter = object : PagerAdapter() {
            private val views = arrayOfNulls<ViewGroup>(pages.size)

            override fun getCount(): Int = pages.size

            override fun getPageTitle(position: Int): CharSequence = when (position) {
                0 -> getString(R.string.lite_about_user_agreement_tab)
                else -> getString(R.string.lite_about_privacy_agreement_tab)
            }

            override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj

            override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
                container.removeView(obj as View)
                views[position] = null
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                var pageView = views[position]
                if (pageView == null) {
                    val page = LiteAboutDynamicUserAgreementBinding.inflate(
                        layoutInflater,
                        container,
                        false,
                    )
                    page.root.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    page.liteAboutText.text = pages[position]
                    pageView = page.root
                    views[position] = pageView
                }
                container.addView(pageView)
                return pageView
            }
        }
        root.liteAboutTabs.setupWithViewPager(root.liteAboutPager)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
