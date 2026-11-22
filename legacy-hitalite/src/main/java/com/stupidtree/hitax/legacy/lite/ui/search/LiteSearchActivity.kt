package com.stupidtree.hitax.legacy.lite.ui.search

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteActivitySearchBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseTabAdapter

/**
 * HITA_Lite 搜索页。
 *
 * 只保留教师检索；输入与结果仍走宿主 TeacherService，不复制 Lite 旧抓取端口。
 */
class LiteSearchActivity : BaseActivity<LiteSearchViewModel, LiteActivitySearchBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViewBinding(): LiteActivitySearchBinding =
        LiteActivitySearchBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteSearchViewModel> = LiteSearchViewModel::class.java

    override fun initViews() {
        binding.toolbar.title = ""
        binding.searchview.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(textView: TextView, action: Int, event: KeyEvent?): Boolean {
                if (textView.text.toString().isBlank()) return false
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_SEARCH) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                    setSearchText(textView.text.toString())
                    return true
                }
                return false
            }
        })
        binding.pager.adapter = SearchPagerAdapter(supportFragmentManager)
        binding.tabs.setupWithViewPager(binding.pager)
    }

    private fun setSearchText(text: String) {
        supportFragmentManager.fragments.forEach {
            if (it is LiteSearchTeacherFragment) it.search(text)
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        binding.searchview.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private class SearchPagerAdapter(fm: FragmentManager) : BaseTabAdapter(fm, 1) {
        override fun getPageTitle(position: Int): CharSequence = "教师"

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            mFragments[position] = null
        }

        override fun initItem(position: Int): Fragment = LiteSearchTeacherFragment()
    }
}
