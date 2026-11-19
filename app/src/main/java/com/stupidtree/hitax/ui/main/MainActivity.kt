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
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.drawerlayout.widget.DrawerLayout.GONE
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.ActivityMainBinding
import com.stupidtree.hitax.market.MarketPreset
import com.stupidtree.hitax.market.PluginMarketViewModel
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

    private var redirectedToShell = false
    private var hitaLMode = false

    /**
     * 插件贡献的抽屉项。id 由宿主按顺序分配，避免菜单 xml 里写死。
     */
    private fun pluginHost() = (application as HApplication).pluginHost

    private val pluginMenuIds = LinkedHashMap<Int, NavEntry>()
    private val pluginTextMenuIds = LinkedHashMap<Int, com.stupidtree.hita.api.TextNavEntry>()

    private val baseTabIds = setOf("hita.timeline", "hita.timetable")

    /** 当前整合包是否声明了自己的底栏布局（复刻型预设）；为真时不再拼内置 4 基页。 */
    private var layoutMode = false
    private var presetBackfillRecreated = false

    private data class Page(
        val id: Int,
        val title: String,
        val iconRes: Int,
        val icon: android.graphics.drawable.Drawable? = null,
        val create: () -> Fragment,
    )

    private var pages: List<Page> = emptyList()

    private fun attachPluginNavItems() {
        val menu = binding.drawerNavigationview.menu
        val host = (application as HApplication).pluginHost
        pluginMenuIds.clear()
        pluginTextMenuIds.clear()
        var index = 0
        host.nav.entries().forEach { entry ->
            val itemId = PLUGIN_MENU_ID_BASE + index
            pluginMenuIds[itemId] = entry
            val title = if (entry.titleRes != 0) getString(entry.titleRes) else entry.id
            val item = menu.add(Menu.NONE, itemId, 100 + index, title)
            item.setIcon(if (entry.iconRes != 0) entry.iconRes else R.drawable.ic_nav_plugin)
            index++
        }
        host.nav.textEntries().forEach { entry ->
            val itemId = PLUGIN_MENU_ID_BASE + index
            pluginTextMenuIds[itemId] = entry
            val item = menu.add(Menu.NONE, itemId, 100 + index, entry.title)
            val icon = entry.icon
            if (icon != null) item.icon = icon
            else item.setIcon(R.drawable.ic_nav_plugin)
            index++
        }
    }

    /**
     * 抽屉里的View
     */
    private val autoReimportIntervalMs = 12 * 60 * 60 * 1000L
    private var autoReimportAttempted = false
    private var pendingUpdateCode: Long? = null
    private var drawerAvatar: ImageView? = null
    private var drawerNickname: TextView? = null
    private var drawerUsername: TextView? = null
    private var drawerHeader: ViewGroup? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && redirectToActiveShell()) return
        if (com.stupidtree.hitax.market.PresetStateStore.activeId(this) == "hita-l-1.5.3") {
            window.navigationBarColor = Color.BLACK
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = false
        }
        setSupportActionBar(binding.toolbar)
        maybeAskUsageConsent()
    }

    private fun maybeAskUsageConsent() {
        if (com.stupidtree.hitax.data.source.preference.UsageStatsStore.hasAsked(this)) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("匿名使用统计")
            .setMessage(
                "是否允许上报匿名安装 ID、版本、事件名、页面和插件 ID？" +
                    "不会上报账号、学号、姓名、课程内容或自由文本。默认关闭，可随时在更多页关闭或清除。"
            )
            .setPositiveButton("允许匿名统计") { _, _ ->
                com.stupidtree.hitax.data.source.preference.UsageStatsStore.setEnabled(this, true)
                com.stupidtree.hitax.analytics.UsageAnalytics.record(
                    this, com.stupidtree.hitax.analytics.UsageEventAllowlist.APP_OPEN
                )
            }
            .setNegativeButton("暂不") { _, _ ->
                com.stupidtree.hitax.data.source.preference.UsageStatsStore.markAsked(this)
            }
            .setOnCancelListener {
                com.stupidtree.hitax.data.source.preference.UsageStatsStore.markAsked(this)
            }
            .show()
    }


    override fun onResume() {
        super.onResume()
        redirectToActiveShell()
    }

    /**
     * 从市场返回时也要立即切换壳；只依赖 onCreate 会让用户停在旧界面上，
     * 直到下一次冷启动才进入 Shell。
     */
    private fun redirectToActiveShell(): Boolean {
        if (redirectedToShell || isFinishing || intent.getBooleanExtra(EXTRA_SKIP_SHELL_REDIRECT, false)) {
            return false
        }
        val shell = com.stupidtree.hitax.market.PresetStateStore.activeShell(this)
        if (shell.isNullOrBlank()) return false
        redirectedToShell = true
        startActivity(
            Intent(this, com.stupidtree.hitax.shell.PresetShellActivity::class.java)
                .putExtra(com.stupidtree.hitax.shell.PresetShellActivity.EXTRA_SHELL_ID, shell)
        )
        finish()
        return true
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
                R.id.drawer_nav_market -> {
                    // 市场不再占底栏槽位（复刻型预设没有这个 tab），统一从抽屉进入
                    startActivity(
                        android.content.Intent(getThis(), com.stupidtree.hitax.market.PluginMarketActivity::class.java)
                    )
                }
                else -> {
                    val entry = pluginMenuIds[item.itemId]
                    val textEntry = pluginTextMenuIds[item.itemId]
                    when {
                        entry != null -> entry.createIntent(getThis())?.let { startActivity(it) }
                        textEntry != null -> textEntry.createIntent(getThis())?.let { startActivity(it) }
                        else -> jumped = false
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
            if (!presetOnboardingSeen()) {
                pendingUpdateCode = code
            } else {
                if (System.currentTimeMillis() - lastCheckTs > 5 * 60 * 1000) checkedUpdate = false
                if (!checkedUpdate) {
                    if (LocalUserRepository.getInstance(this).getLoggedInUser().isValid()) {
                        checkedUpdate = true
                        lastCheckTs = System.currentTimeMillis()
                    }
                    viewModel.checkForUpdate(code)
                }
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
        maybeShowPresetOnboarding()
        backfillPresetLayoutOnStartup()
        //  binding.title.text = binding.navView.menu.getItem(0).title
        pages = buildPages()
        hitaLMode = com.stupidtree.hitax.market.PresetStateStore.activeId(this) == "hita-l-1.5.3"
        if (hitaLMode) {
            binding.navView.visibility = VISIBLE
            binding.navViewExtended.visibility = GONE
            binding.navView.itemActiveIndex = 0
            binding.navView.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelect(pos: Int): Boolean {
                    binding.navView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    binding.pager.currentItem = pos
                    return true
                }
            }
        } else {
            binding.navView.visibility = GONE
            binding.navViewExtended.visibility = VISIBLE
            binding.navViewExtended.submit(
                pages.map { DynamicBottomBar.Item(it.id, it.title, it.iconRes, it.icon) }
            )
            binding.navViewExtended.onItemSelected = { index ->
                binding.navViewExtended.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                binding.pager.currentItem = index
                true
            }
        }
        binding.pager.adapter = object : BaseTabAdapter(supportFragmentManager, pages.size) {
            override fun initItem(position: Int): Fragment = pages[position].create()

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                //super.destroyItem(container, position, `object`)
            }
        }
        binding.pager.offscreenPageLimit = pages.size
        binding.pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                if (hitaLMode) binding.navView.itemActiveIndex = position
                else binding.navViewExtended.select(position)
                showHeaderFor(position)
                com.stupidtree.hitax.analytics.UsageAnalytics.record(
                    this@MainActivity,
                    com.stupidtree.hitax.analytics.UsageEventAllowlist.SCREEN_VIEW,
                    screen = listOf("timeline", "timetable", "navigation").getOrNull(position),
                )
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding.drawerButton.setOnClickListener { binding.drawer.openDrawer(GravityCompat.END) }
        binding.runtimeDrawerButton.setOnClickListener { binding.drawer.openDrawer(GravityCompat.END) }
        // ViewPager2 初始不会回调 onPageSelected；布局模式下必须主动切到 runtime header。
        showHeaderFor(binding.pager.currentItem)

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


    private fun presetOnboardingSeen(): Boolean =
        getSharedPreferences("preset_onboarding", MODE_PRIVATE)
            .getBoolean(KEY_PRESET_PROMPT_SEEN, false)

    private fun checkPendingUpdate() {
        val code = pendingUpdateCode ?: return
        pendingUpdateCode = null
        if (System.currentTimeMillis() - lastCheckTs > 5 * 60 * 1000) checkedUpdate = false
        if (!checkedUpdate) {
            if (LocalUserRepository.getInstance(this).getLoggedInUser().isValid()) {
                checkedUpdate = true
                lastCheckTs = System.currentTimeMillis()
            }
            viewModel.checkForUpdate(code)
        }
    }

    /**
     * 首次启动直接选择整合包；选择后应用并记录当前组合。
     * 只提示一次，之后可在市场切换。
     */
    private fun maybeShowPresetOnboarding() {
        val prefs = getSharedPreferences("preset_onboarding", MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PRESET_PROMPT_SEEN, false)) return
        if (isFinishing || supportFragmentManager.isStateSaved) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("欢迎使用 OH-MY-HITA")
            .setMessage(
                "选择一个整合包开始：\n\n" +
                    "· HITA-L：经典轻量组合\n" +
                    "· HITA_Android 3.1.2：完整复刻组合\n\n" +
                    "也可以先从零开始，之后在插件市场随时切换。"
            )
            .setPositiveButton("选择整合包") { _, _ ->
                prefs.edit().putBoolean(KEY_PRESET_PROMPT_SEEN, true).apply()
                showPresetChooser()
            }
            .setNegativeButton("从零开始") { _, _ ->
                prefs.edit().putBoolean(KEY_PRESET_PROMPT_SEEN, true).apply()
                checkPendingUpdate()
            }
            .setNeutralButton("稍后", null)
            .show()
    }

    /**
     * 存量用户的底栏补写必须在普通启动路径触发：
     * 仅当已启用整合包且本地 tabs 为空或缺少 iconAsset 时创建市场 ViewModel，
     * 索引加载完成后由 PresetLayoutSync 写入布局并发出 reloadUi。
     */
    private fun backfillPresetLayoutOnStartup() {
        val activeId = com.stupidtree.hitax.market.PresetStateStore.activeId(this) ?: return
        if (activeId.isBlank()) return
        val stored = com.stupidtree.hitax.market.PresetLayoutStore.tabs(this)
        // 布局已完整（含图标）才跳过；缺 iconAsset 时也要启动同步，否则存量用户永远用回退图标。
        if (stored.isNotEmpty() && stored.none { it.iconAsset.isNullOrBlank() }) return
        val vm = ViewModelProvider(this)[PluginMarketViewModel::class.java]
        vm.reloadUi.observe(this) { reload ->
            if (reload && !isFinishing && !presetBackfillRecreated) {
                presetBackfillRecreated = true
                vm.consumeReloadUi()
                recreate()
            }
        }
    }

    /** 读取市场索引并弹出整合包列表；失败时回退到插件市场。 */
    private fun showPresetChooser() {
        val vm = ViewModelProvider(this)[PluginMarketViewModel::class.java]
        var applying: androidx.appcompat.app.AlertDialog? = null
        var applyStarted = false
        var finished = false
        var fellBack = false
        var recreationRequested = false
        lateinit var presetsObserver: Observer<List<MarketPreset>>
        lateinit var statusObserver: Observer<String?>
        lateinit var loadedObserver: Observer<Boolean>
        lateinit var loading: androidx.appcompat.app.AlertDialog

        fun finishOnboarding(checkUpdate: Boolean = true) {
            if (finished) return
            finished = true
            if (checkUpdate) checkPendingUpdate()
        }

        fun recreateOnce() {
            if (recreationRequested || isFinishing) return
            recreationRequested = true
            recreate()
        }

        fun fallback(message: String) {
            if (fellBack) return
            fellBack = true
            vm.presets.removeObserver(presetsObserver)
            vm.status.removeObserver(statusObserver)
            vm.presetsLoaded.removeObserver(loadedObserver)
            loading.dismiss()
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finishOnboarding(checkUpdate = false)
            openPluginMarket()
        }

        fun cancelLoading() {
            vm.presets.removeObserver(presetsObserver)
            vm.status.removeObserver(statusObserver)
            vm.presetsLoaded.removeObserver(loadedObserver)
            loading.dismiss()
            finishOnboarding()
        }

        loading = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("整合包")
            .setMessage("正在读取市场索引…")
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancelLoading() }
            .show()

        presetsObserver = Observer { presets ->
            if (presets.isNullOrEmpty()) return@Observer
            vm.presets.removeObserver(presetsObserver)
            vm.status.removeObserver(statusObserver)
            vm.presetsLoaded.removeObserver(loadedObserver)
            loading.dismiss()
            var checked = 0
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("选择整合包")
                .setSingleChoiceItems(
                    presets.map { "${it.name}（${it.refs().size} 个插件）" }.toTypedArray(),
                    checked,
                ) { _, which -> checked = which }
                .setPositiveButton("应用") { _, _ ->
                    val preset = presets.getOrNull(checked) ?: return@setPositiveButton
                    applyStarted = true
                    applying = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("正在应用")
                        .setMessage("正在安装「${preset.name}」，请稍候…")
                        .setCancelable(false)
                        .show()
                    vm.applyPreset(preset)
                }
                .setNegativeButton("稍后", null)
                .setOnDismissListener { if (!applyStarted) finishOnboarding() }
                .show()
        }
        statusObserver = Observer { text ->
            if (text?.startsWith("读取索引失败") == true) {
                fallback("读取市场失败，请到插件市场重试")
            }
        }
        loadedObserver = Observer { loaded ->
            if (loaded && vm.presets.value.isNullOrEmpty()) {
                fallback("市场暂无整合包，已打开插件市场")
            }
        }
        vm.presets.observe(this, presetsObserver)
        vm.status.observe(this, statusObserver)
        vm.presetsLoaded.observe(this, loadedObserver)
        vm.activePresetId.observe(this) { id ->
            if (applyStarted && id != null) recreateOnce()
        }
        vm.message.observe(this) { text ->
            if (text == null) return@observe
            applying?.dismiss()
            applying = null
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
            if (applyStarted) finishOnboarding()
        }
        vm.reloadUi.observe(this) { reload ->
            if (reload) {
                vm.consumeReloadUi()
                val shell = com.stupidtree.hitax.market.PresetStateStore.activeShell(this)
                if (!shell.isNullOrBlank()) {
                    startActivity(
                        Intent(this, com.stupidtree.hitax.shell.PresetShellActivity::class.java)
                            .putExtra(
                                com.stupidtree.hitax.shell.PresetShellActivity.EXTRA_SHELL_ID,
                                shell,
                            )
                    )
                    finish()
                } else {
                    recreateOnce()
                }
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    /** 供占位页跳转到插件市场；布局模式下市场已脱离底栏，必须开独立页面。 */
    fun openPluginMarket() {
        if (layoutMode) {
            startActivity(Intent(getThis(), com.stupidtree.hitax.market.PluginMarketActivity::class.java))
        } else {
            binding.pager.currentItem = 3
        }
    }

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
     * 页面头互斥显示：顺序与 pager 的页序一一对应
     * （0 今日 / 1 时间表 / 2 功能中心 / 3 插件市场 / 4 插件贡献页）。
     * 新增标签页时只需在这里加一项，避免某个分支漏关导致标题叠加。
     */
    private fun buildPages(): List<Page> {
        val host = (application as HApplication).pluginHost
        val layout = com.stupidtree.hitax.market.PresetLayoutStore.tabs(this)
        layoutMode = layout.isNotEmpty()
        if (layoutMode) return layoutPages(layout)
        val result = mutableListOf(
            Page(
                R.id.navigation_timeline,
                getString(R.string.title_timeline),
                R.drawable.ic_nav_today,
            ) {
                host.tabs.find("hita.timeline")?.let { it.create() }
                    ?: PluginUnavailableFragment.newInstance("hita.timeline", getString(R.string.title_timeline))
            },
            Page(
                R.id.navigation_timetable,
                getString(R.string.title_timetable),
                R.drawable.ic_nav_timetable,
            ) {
                host.tabs.find("hita.timetable")?.let { it.create() }
                    ?: PluginUnavailableFragment.newInstance("hita.timetable", getString(R.string.title_timetable))
            },
            Page(R.id.navigation_navigation, getString(R.string.title_navigation), R.drawable.ic_nav_navigation) {
                NavigationFragment()
            },
        )
        if (com.stupidtree.hitax.market.PresetStateStore.activeId(this) == "hita-l-1.5.3") {
            // HITA-L 1.5.3 固定三页；SmoothBottomBar 菜单也是静态三项，不能追加插件页。
            return result
        }
        result += Page(
            R.id.navigation_plugin,
            getString(R.string.title_plugin),
            R.drawable.ic_nav_plugin,
        ) {
            com.stupidtree.hitax.market.FragmentPluginMarket()
        }
        host.tabs.entries()
            .filterNot { it.id in baseTabIds }
            .forEach { tab ->
                result += Page(
                    1000 + result.size,
                    if (tab.titleRes != 0) getString(tab.titleRes) else tab.id,
                    if (tab.iconRes != 0) tab.iconRes else R.drawable.ic_nav_plugin,
                    create = tab.create,
                )
            }
        val uiPluginManager = com.stupidtree.hitax.uiplugin.UiPluginManager(this)
        uiPluginManager.mainTabs().forEach { installed ->
            val title = installed.spec.title.ifBlank { installed.plugin.name }
            result += Page(
                2000 + result.size,
                title,
                R.drawable.ic_nav_plugin,
                icon = uiPluginManager.loadMainTabIcon(installed.plugin, installed.spec.iconAsset),
            ) {
                com.stupidtree.hitax.uiplugin.RuntimeUiTabFragment.newInstance(
                    installed.plugin.id,
                    installed.spec,
                )
            }
        }
        return result
    }

    /**
     * 按整合包声明的 tabs 组装底栏。
     *
     * 复刻型预设（hita-android-3.1.2）用它把底栏固定成与上游一致的 6 个位置，
     * 顺序、标题都由预设决定，不受插件注册顺序影响。
     */
    private fun layoutPages(tabs: List<com.stupidtree.hitax.market.PresetTab>): List<Page> {
        val host = (application as HApplication).pluginHost
        val uiManager = com.stupidtree.hitax.uiplugin.UiPluginManager(this)
        val uiTabs = uiManager.mainTabs().associateBy { it.plugin.id }
        return tabs.sortedBy { it.order }.mapIndexedNotNull { index, tab ->
            val title = tab.title?.takeIf { it.isNotBlank() } ?: tab.id
            when {
                tab.id == "hita.timeline" -> Page(
                    R.id.navigation_timeline,
                    title,
                    com.stupidtree.hitax.market.BuiltinTabIcons.resolve(tab.iconAsset)
                        ?: R.drawable.ic_nav_today,
                ) {
                    host.tabs.find("hita.timeline")?.let { it.create() }
                        ?: PluginUnavailableFragment.newInstance("hita.timeline", title)
                }

                tab.id == "hita.timetable" -> Page(
                    R.id.navigation_timetable,
                    title,
                    com.stupidtree.hitax.market.BuiltinTabIcons.resolve(tab.iconAsset)
                        ?: R.drawable.ic_nav_timetable,
                ) {
                    host.tabs.find("hita.timetable")?.let { it.create() }
                        ?: PluginUnavailableFragment.newInstance("hita.timetable", title)
                }

                tab.id == "hita.more" -> Page(
                    R.id.navigation_navigation,
                    title,
                    com.stupidtree.hitax.market.BuiltinTabIcons.resolve(tab.iconAsset)
                        ?: R.drawable.ic_nav_navigation,
                ) { NavigationFragment() }

                uiTabs[tab.id] != null -> {
                    val installed = uiTabs.getValue(tab.id)
                    // 预设写 builtin: 时用宿主内置图标，否则退回插件自带图标
                    val builtinIcon = com.stupidtree.hitax.market.BuiltinTabIcons.resolve(tab.iconAsset)
                    Page(
                        2000 + index,
                        title,
                        builtinIcon ?: R.drawable.ic_nav_plugin,
                        icon = if (builtinIcon != null) {
                            null
                        } else {
                            uiManager.loadMainTabIcon(
                                installed.plugin,
                                tab.iconAsset
                                    ?.takeUnless(com.stupidtree.hitax.market.BuiltinTabIcons::isBuiltin)
                                    ?: installed.spec.iconAsset,
                            )
                        },
                    ) {
                        com.stupidtree.hitax.uiplugin.RuntimeUiTabFragment.newInstance(
                            installed.plugin.id,
                            installed.spec,
                        )
                    }
                }

                else -> {
                    val contribution = host.tabs.find(tab.id)
                    val icon = com.stupidtree.hitax.market.BuiltinTabIcons.resolve(tab.iconAsset)
                        ?: contribution?.iconRes?.takeIf { it != 0 }
                        ?: R.drawable.ic_nav_plugin
                    if (contribution != null) {
                        Page(1000 + index, title, icon, create = contribution.create)
                    } else {
                        // 预设声明了但对应插件没装：给占位页而不是丢掉这个 tab，
                        // 否则底栏会少一格，与 3.1.2 的固定 6 个入口对不上。
                        Page(3000 + index, title, icon) {
                            PluginUnavailableFragment.newInstance(tab.id, title)
                        }
                    }
                }
            }
        }
    }

    private fun showHeaderFor(position: Int) {
        if (layoutMode) {
            // 布局模式下页面自带标题，宿主只保留一个通用 header 显示当前页名
            listOf(
                binding.todayLayout,
                binding.timetableLayout,
                binding.navigationLayout,
                binding.pluginLayout,
            ).forEach { it.visibility = GONE }
            binding.runtimeLayout.visibility = VISIBLE
            binding.runtimeTitle.text = pages.getOrNull(position)?.title.orEmpty()
            return
        }
        val headers = listOf(
            binding.todayLayout,
            binding.timetableLayout,
            binding.navigationLayout,
            binding.pluginLayout,
        )
        headers.forEachIndexed { index, header ->
            header.visibility = if (index == position) VISIBLE else GONE
        }
        val runtimePage = position >= headers.size
        binding.runtimeLayout.visibility = if (runtimePage) VISIBLE else GONE
        if (runtimePage) {
            binding.runtimeTitle.text = pages.getOrNull(position)?.title.orEmpty()
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
        /** Shell 内打开“应用设置”时使用，避免立即被 onResume 重新路由回 Shell。 */
        const val EXTRA_SKIP_SHELL_REDIRECT = "skipShellRedirect"

        /** 插件菜单项 id 从这里开始分配，避开 xml 里的资源 id 区间。 */
        private const val PLUGIN_MENU_ID_BASE = 0x0F000000
        private const val KEY_PRESET_PROMPT_SEEN = "preset_prompt_seen"
    }
}
