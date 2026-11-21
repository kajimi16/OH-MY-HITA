package com.stupidtree.hitax.legacy.lite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.stupidtree.hitax.legacy.lite.databinding.LiteActivityMainBinding
import me.ibrahimsn.lib.OnItemSelectedListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HITA_Lite legacy shell.
 *
 * 页面本身使用宿主 HITA-L 同源 Fragment；Lite 只保留三页静态底栏与独立壳。
 * 数据、登录、数据库和组件注册一律不迁入，统一走宿主服务。
 */
class HitaLiteActivity : AppCompatActivity() {

    private lateinit var binding: LiteActivityMainBinding

    private val pageClasses = arrayOf(
        "com.stupidtree.hitax.ui.main.timeline.FragmentTimeLine",
        "com.stupidtree.hitax.ui.main.timetable.TimetableFragment",
        "com.stupidtree.hitax.ui.main.navigation.NavigationFragment",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LiteActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                binding.liteNav.itemActiveIndex = position
                updatePageTitle(position)
            }
        })
        binding.liteNav.itemActiveIndex = 0
        binding.liteNav.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelect(pos: Int): Boolean {
                binding.litePager.currentItem = pos
                return true
            }
        }
        updatePageTitle(0)
    }

    private fun updatePageTitle(position: Int) {
        binding.litePageTitle.text = when (position) {
            0 -> SimpleDateFormat("M月d日 EEE", Locale.CHINA).format(Date())
            1 -> getString(R.string.lite_title_timetable)
            else -> getString(R.string.lite_title_navigation)
        }
    }

    private fun hostFragment(className: String): Fragment? = runCatching {
        Class.forName(className, true, applicationContext.classLoader)
            .getDeclaredConstructor()
            .newInstance() as Fragment
    }.getOrNull()
}
