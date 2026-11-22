package com.stupidtree.hitax.legacy.lite

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.stupidtree.hitax.legacy.lite.databinding.LiteActivityMainBinding
import com.stupidtree.style.ThemeTools
import com.stupidtree.style.base.TimelinePageController
import com.stupidtree.style.base.TimetablePageController
import me.ibrahimsn.lib.OnItemSelectedListener

/**
 * HITA_Lite legacy shell.
 *
 * 三页与标题/抽屉/底栏按 HITA_Lite v2.1 的 activity_main 还原；页面本身复用宿主
 * 插件里的 Lite 同源 Fragment。数据、登录、数据库和组件注册不进入本模块。
 *
 * 契约：
 * - 前置条件：`LiteHostBridge` 已注入（或允许为空）。
 * - 后置条件：三页可切换，标题随页面和宿主 Fragment 回调更新。
 * - 不变式：不创建第二套 Room/EAS/登录/Widget receiver。
 */
class HitaLiteActivity : AppCompatActivity(), TimetablePageController, TimelinePageController {

    private lateinit var binding: LiteActivityMainBinding

    private val pageClasses = arrayOf(
        "com.stupidtree.hitax.ui.main.timeline.FragmentTimeLine",
        "com.stupidtree.hitax.ui.main.timetable.TimetableFragment",
        "com.stupidtree.hitax.legacy.lite.ui.navigation.LiteNavigationFragment",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LiteActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.liteToolbar)
        setUpDrawer()
        setUpPager()
        setUpActions()
        showPage(0)
        refreshThemeIcon()
    }

    override fun onStart() {
        super.onStart()
        refreshThemeIcon()
    }

    private fun refreshThemeIcon() {
        binding.liteSwitchTheme.setImageResource(
            when (ThemeTools.getThemeMode(this)) {
                ThemeTools.MODE.DARK -> R.drawable.lite_ic_moon2
                ThemeTools.MODE.LIGHT -> R.drawable.lite_ic_sun
                else -> R.drawable.lite_ic_moon_auto
            }
        )
    }

    private fun setUpDrawer() {
        binding.liteDrawerNavigationview.itemIconTintList = null
        binding.liteDrawer.setStatusBarBackgroundColor(Color.TRANSPARENT)
        binding.liteDrawer.setScrimColor(getThemeColor(android.R.attr.colorControlNormal))
        binding.liteDrawer.drawerElevation = resources.displayMetrics.density * 84f
        binding.liteDrawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val content = binding.liteDrawer.getChildAt(0)
                val scale = 1 - slideOffset
                val rightScale = 0.8f + scale * 0.2f
                content.translationX = -drawerView.measuredWidth * slideOffset
                content.pivotX = content.measuredWidth.toFloat()
                content.pivotY = (content.measuredHeight shr 1).toFloat()
                content.invalidate()
                content.scaleX = rightScale
                content.scaleY = rightScale
            }

            override fun onDrawerOpened(drawerView: View) = Unit
            override fun onDrawerClosed(drawerView: View) = Unit
            override fun onDrawerStateChanged(newState: Int) = Unit
        })
        binding.liteDrawerNavigationview.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.lite_drawer_nav_ua -> {
                    LiteHostBridge.openAgreement?.invoke(supportFragmentManager)
                    true
                }
                R.id.lite_drawer_nav_market -> {
                    LiteHostBridge.openMarket?.invoke(this)
                    true
                }
                R.id.lite_drawer_nav_about -> {
                    LiteHostBridge.openAbout?.invoke(this)
                    true
                }
                else -> false
            }
        }
    }

    private fun setUpPager() {
        binding.litePager.adapter = object : FragmentStatePagerAdapter(
            supportFragmentManager,
            BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
        ) {
            override fun getCount(): Int = pageClasses.size

            override fun getItem(position: Int): Fragment =
                hostFragment(pageClasses[position]) ?: Fragment()

            override fun getPageTitle(position: Int): CharSequence? = when (position) {
                0 -> getString(R.string.lite_title_timeline)
                1 -> getString(R.string.lite_title_timetable)
                else -> getString(R.string.lite_title_navigation)
            }
        }
        binding.litePager.offscreenPageLimit = pageClasses.size
        binding.litePager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                showPage(position)
            }
        })
        binding.liteNav.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelect(pos: Int): Boolean {
                binding.liteNav.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                binding.litePager.currentItem = pos
                return true
            }
        }
    }

    private fun setUpActions() {
        binding.liteDrawerButton.setOnClickListener {
            binding.liteDrawer.openDrawer(GravityCompat.END)
        }
        binding.liteSwitchTheme.setOnClickListener {
            ThemeTools.switchTheme(this)
        }
        binding.liteTimetableSetting.setOnClickListener {
            // 宿主面板来自运行时课表插件；缺省时保持空操作，不复制 Lite 旧主题单例。
        }
        binding.liteAddEvent.setOnClickListener {
            LiteHostBridge.openAddEvent?.invoke(this)
        }
    }

    private fun showPage(position: Int) {
        binding.liteNav.itemActiveIndex = position
        when (position) {
            0 -> {
                binding.liteTodayLayout.visibility = VISIBLE
                binding.liteTimetableLayout.visibility = GONE
                binding.liteNavigationLayout.visibility = GONE
            }
            1 -> {
                binding.liteTodayLayout.visibility = GONE
                binding.liteTimetableLayout.visibility = VISIBLE
                binding.liteNavigationLayout.visibility = GONE
            }
            else -> {
                binding.liteTodayLayout.visibility = GONE
                binding.liteTimetableLayout.visibility = GONE
                binding.liteNavigationLayout.visibility = VISIBLE
            }
        }
    }

    override fun setTimelineTitleText(string: String) {
        binding.liteTodayTitle.text = string
    }

    override fun setTitleText(string: String) {
        binding.liteTimetableTitle.text = string
        binding.liteTimetableNameCard.visibility = VISIBLE
    }

    override fun setTimetableName(string: String) {
        binding.liteTimetableName.text = string
        binding.liteTimetableNameCard.visibility = VISIBLE
    }

    override fun setSingleTitle(string: String) {
        binding.liteTimetableTitle.text = string
        binding.liteTimetableNameCard.visibility = GONE
    }

    override fun onBackPressed() {
        if (binding.liteDrawer.isDrawerOpen(GravityCompat.END)) {
            binding.liteDrawer.closeDrawer(GravityCompat.END)
            return
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(intent)
    }

    private fun getThemeColor(attr: Int): Int {
        val value = android.util.TypedValue()
        theme.resolveAttribute(attr, value, true)
        return value.data
    }

    private fun hostFragment(className: String): Fragment? = runCatching {
        Class.forName(className, true, applicationContext.classLoader)
            .getDeclaredConstructor()
            .newInstance() as Fragment
    }.getOrNull()
}
