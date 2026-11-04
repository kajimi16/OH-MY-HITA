package com.stupidtree.hitax.ui.main

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.drawerlayout.widget.DrawerLayout.GONE
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.ActivityMainBinding
import com.stupidtree.hitax.ui.about.ActivityAbout
import com.stupidtree.hitax.ui.about.UserAgreementDialog
import com.stupidtree.hitax.ui.main.navigation.NavigationFragment
import com.stupidtree.hitax.utils.ActivityUtils
import com.stupidtree.hitax.utils.ImageUtils
import com.stupidtree.stupiduser.data.repository.LocalUserRepository
import com.stupidtree.style.ThemeTools
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseTabAdapter
import com.stupidtree.style.widgets.PopUpText
import me.ibrahimsn.lib.OnItemSelectedListener

/**
 * 很显然，这是主界面
 */
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>(),
    com.stupidtree.style.base.TimetablePageController,
    com.stupidtree.style.base.TimelinePageController {

    /**
     * 插件贡献的抽屉项。id 由宿主按顺序分配，避免菜单 xml 里写死。
     */
    private fun pluginHost() = (application as HApplication).pluginHost

    private val pluginMenuIds = LinkedHashMap<Int, NavEntry>()

    private fun attachPluginNavItems() {
        val menu = binding.drawerNavigationview.menu
        val host = (application as HApplication).pluginHost
        pluginMenuIds.clear()
        host.nav.entries().forEachIndexed { index, entry ->
            val itemId = PLUGIN_MENU_ID_BASE + index
            pluginMenuIds[itemId] = entry
            menu.add(Menu.NONE, itemId, 100 + index, entry.titleRes)
                .setIcon(entry.iconRes)
        }
    }

    /**
     * 抽屉里的View
     */
    private val autoReimportIntervalMs = 12 * 60 * 60 * 1000L
    private var autoReimportAttempted = false
    private var drawerAvatar: ImageView? = null
    private var drawerNickname: TextView? = null
    private var drawerUsername: TextView? = null
    private var drawerHeader: ViewGroup? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
    }


    private fun setUpDrawer() {
        binding.drawerNavigationview.itemIconTintList = null
        val headerView =
            binding.drawerNavigationview.inflateHeaderView(R.layout.activity_main_nav_header)
        binding.drawer.setStatusBarBackgroundColor(Color.TRANSPARENT)
        binding.drawer.setScrimColor(getBackgroundColorSecondAsTint())
        binding.drawer.drawerElevation = ImageUtils.dp2px(this, 84f).toFloat()
        drawerAvatar = headerView.findViewById(R.id.avatar)
        drawerHeader = headerView.findViewById(R.id.drawer_header)
        drawerNickname = headerView.findViewById(R.id.nickname)
        drawerUsername = headerView.findViewById(R.id.username)
        attachPluginNavItems()
        binding.drawer.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                //offset 偏移值
                val mContent = binding.drawer.getChildAt(0)
                val scale = 1 - slideOffset
                val rightScale = 0.8f + scale * 0.2f
                mContent.translationX = -drawerView.measuredWidth * slideOffset
                mContent.pivotX = mContent.measuredWidth.toFloat()
                mContent.pivotY = (mContent.measuredHeight shr 1.toFloat().toInt()).toFloat()
                mContent.invalidate()
                mContent.scaleX = rightScale
                mContent.scaleY = rightScale
            }

            override fun onDrawerOpened(drawerView: View) {
                // setUserViews(viewModel.localUser)
            }

            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        binding.drawerNavigationview.setNavigationItemSelectedListener { item: MenuItem ->
            var jumped = true
            when (item.itemId) {
                R.id.drawer_nav_ua -> {
                    UserAgreementDialog().show(supportFragmentManager, "ua")
                }
                R.id.drawer_nav_timetable_manager -> {
                    ActivityUtils.startTimetableManager(getThis())
                }
                R.id.drawer_nav_about -> {
                    ActivityUtils.startActivity(getThis(), ActivityAbout::class.java)
                }
                else -> {
                    val entry = pluginMenuIds[item.itemId]
                    if (entry == null) {
                        jumped = false
                    } else {
                        entry.createIntent(getThis())?.let { startActivity(it) }
                    }
                }
            }
//            if (jumped) {
//                binding.drawer.closeDrawer(GravityCompat.START)
//            }
            jumped
        }
    }

    var checkedUpdate = false
    var lastCheckTs: Long = 0

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        viewModel.startRefreshUser()
        refreshTheme()
        maybeAutoReimportTimetable()
        try {
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    0
                ).longVersionCode
            } else {
                packageManager.getPackageInfo(
                    packageName,
                    0
                ).versionCode.toLong()
            }
            if (System.currentTimeMillis() - lastCheckTs > 5 * 60 * 1000) checkedUpdate = false
            if (!checkedUpdate) {
                if (LocalUserRepository.getInstance(this).getLoggedInUser().isValid()) {
                    checkedUpdate = true
                    lastCheckTs = System.currentTimeMillis()
                }
                viewModel.checkForUpdate(code)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun maybeAutoReimportTimetable() {
        if (autoReimportAttempted) return
        autoReimportAttempted = true
        // 间隔与登录判断都在插件内完成
        pluginHost().service(com.stupidtree.hita.api.EasService.KEY)?.autoReimportIfDue()
    }


    override fun initViews() {
        setUpDrawer()
        //  binding.title.text = binding.navView.menu.getItem(0).title
        binding.pager.adapter = object : BaseTabAdapter(supportFragmentManager, 4) {
            override fun initItem(position: Int): Fragment {
                return when (position) {
                    0 -> (application as HApplication).pluginHost.tabs
                        .find("hita.timeline")?.let { it.create() } ?: Fragment()
                    1 -> (application as HApplication).pluginHost.tabs
                        .find("hita.timetable")?.let { it.create() } ?: Fragment()
                    2 -> NavigationFragment()
                    else -> com.stupidtree.hitax.market.FragmentPluginMarket()
                }
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                //super.destroyItem(container, position, `object`)
            }
        }
        binding.pager.offscreenPageLimit = 4
        binding.pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                binding.navView.itemActiveIndex = position//.getItem(position).isChecked = true
                showHeaderFor(position)
//                val item = binding.navView.menu.getItem(position)
//                item.isChecked = true
//                binding.title.text = item.title
                //Objects.requireNonNull(getSupportActionBar()).setTitle(item.getTitle());
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding.navView.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelect(pos: Int): Boolean {
                binding.navView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                binding.pager.currentItem = pos
                return true
            }

        }
//        binding.navView.setOnNavigationItemSelectedListener { item: MenuItem ->
//            when (item.itemId) {
//                R.id.navigation_timeline -> binding.pager.currentItem = 0
//                R.id.navigation_timetable -> binding.pager.currentItem = 1
//                R.id.navigation_navigation -> binding.pager.currentItem = 2
//            }
//            binding.navView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
//            true
//        }
        binding.drawerButton.setOnClickListener { binding.drawer.openDrawer(GravityCompat.END) }

        binding.timetableSetting.setOnClickListener {
            pluginHost().service(com.stupidtree.hita.api.TimetableUiService.KEY)
                ?.openStylePanel(this)
        }

        binding.addEvent.setOnClickListener {
            pluginHost().service(com.stupidtree.hita.api.TimetableUiService.KEY)
                ?.openAddEvent(this, null)
        }

        binding.switchTheme.setOnClickListener {
            ThemeTools.switchTheme(getThis())
            pluginHost().service(com.stupidtree.hita.api.TimetableUiService.KEY)
                ?.refreshWidgets(this)
        }
        viewModel.checkUpdateResult.observe(this) {
            if (it.state == DataState.STATE.SUCCESS) {
                it.data?.let { cr ->
                    if (cr.shouldUpdate) {
                        ActivityUtils.showUpdateNotification(cr, this)
                    }
                }
            }
        }
        viewModel.loggedInUserLiveData.observe(this) {
            Log.e("user", it.toString())
            if (it.isValid()) { //如果已登录
                //装载头像
                com.stupidtree.stupiduser.util.ImageUtils.loadAvatarInto(
                    this,
                    it.avatar,
                    drawerAvatar!!
                )
                //设置各种文字
                drawerUsername?.text = it.username
                drawerNickname?.text = it.nickname
                drawerHeader?.setOnClickListener {
                    ActivityUtils.startProfileActivity(
                        getThis(),
                        LocalUserRepository.getInstance(applicationContext).getLoggedInUser().id,
                        drawerAvatar
                    )
                }
            } else {
                //未登录的信息显示
                val eas = pluginHost().service(com.stupidtree.hita.api.EasService.KEY)
                val easName = eas?.accountName()
                if (easName != null) {
                    drawerUsername?.text = easName
                    drawerNickname?.text = eas.accountSubtitle()
                } else {
                    drawerUsername?.setText(R.string.eas_account_not_logged_in_title)
                    drawerNickname?.setText(R.string.eas_account_not_logged_in_subtitle)
                }
                drawerAvatar?.setImageResource(R.drawable.place_holder_avatar)
                drawerHeader?.setOnClickListener { eas?.openAccount(getThis()) }
            }
        }
    }


    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        //super.onBackPressed();
        if (binding.drawer.isDrawerOpen(GravityCompat.END)) {
            binding.drawer.closeDrawer(GravityCompat.END)
            return
        }
        //返回桌面而非退出
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }


    private fun refreshTheme() {
        when (ThemeTools.getThemeMode(this)) {
            ThemeTools.MODE.DARK -> binding.switchTheme.setImageResource(R.drawable.ic_moon2)
            ThemeTools.MODE.LIGHT -> binding.switchTheme.setImageResource(R.drawable.ic_sun)
            else -> binding.switchTheme.setImageResource(R.drawable.ic_moon_auto)
        }
    }

    override fun getViewModelClass(): Class<MainViewModel> {
        return MainViewModel::class.java
    }

    override fun initViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    /**
     * 四个页面头互斥显示：顺序与 pager 的页序一一对应
     * （0 今日 / 1 时间表 / 2 功能中心 / 3 插件市场）。
     * 新增标签页时只需在这里加一项，避免某个分支漏关导致标题叠加。
     */
    private fun showHeaderFor(position: Int) {
        val headers = listOf(
            binding.todayLayout,
            binding.timetableLayout,
            binding.navigationLayout,
            binding.pluginLayout,
        )
        headers.forEachIndexed { index, header ->
            header.visibility = if (index == position) VISIBLE else GONE
        }
    }

    override fun setTitleText(string: String) {
        binding.timetableTitle.text = string
        binding.timetableNameCard.visibility = VISIBLE
    }

    override fun setTimetableName(String: String) {
        binding.timetableName.text = String
        binding.timetableNameCard.visibility = VISIBLE
    }

    override fun setSingleTitle(string: String) {
        binding.timetableTitle.text = string
        binding.timetableNameCard.visibility = GONE
    }

    override fun setTimelineTitleText(string: String) {
        binding.todayTitle.text = string
    }

    companion object {
        /** 插件菜单项 id 从这里开始分配，避开 xml 里的资源 id 区间。 */
        private const val PLUGIN_MENU_ID_BASE = 0x0F000000
    }
}
