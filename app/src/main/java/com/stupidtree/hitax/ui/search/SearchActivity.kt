package com.stupidtree.hitax.ui.search

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.SearchTabIds
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.ActivitySearchBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.base.BaseTabAdapter
import com.stupidtree.hitax.utils.ActivityUtils
import com.stupidtree.style.base.FragmentSearchResult

class SearchActivity : BaseActivity<SearchViewModel, ActivitySearchBinding>(),
    BasicFragmentSearchResult.SearchRoot {
    private var pagerAdapter: SearchPagerAdapter? = null

    private data class SearchPage(
        val id: String,
        val title: String,
        val order: Int,
        val create: () -> Fragment,
    )

    private var pages: List<SearchPage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }


    override fun onStart() {
        super.onStart()
        searchForPurpose()
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        if (!isSearchForPurpose()) {
            popUpKeyboard()
        }
    }


    private fun isSearchForPurpose(): Boolean {
        val text = intent.getStringExtra("keyword")
        val purpose = intent.getStringExtra("type")
        return text?.isNotEmpty() == true && purpose?.isNotEmpty() == true
    }

    private fun searchForPurpose() {
        val text = intent.getStringExtra("keyword")
        binding.searchview.setText(text)
        val purpose = intent.getStringExtra("type")
        if (text.isNullOrEmpty() || purpose.isNullOrEmpty()) return
        val tabId = when (purpose) {
            ActivityUtils.SearchType.TEACHER.name -> SearchTabIds.TEACHER
            ActivityUtils.SearchType.USER.name -> SearchTabIds.USER
            ActivityUtils.SearchType.ARTICLE.name -> SearchTabIds.ARTICLE
            else -> return
        }
        val index = pages.indexOfFirst { it.id == tabId }
        if (index >= 0) binding.pager.currentItem = index
    }

    override fun initViews() {
        binding.toolbar.title = ""
        binding.searchview.setOnEditorActionListener (object : TextView.OnEditorActionListener {
            override fun onEditorAction(textView: TextView, i: Int, keyEvent: KeyEvent?): Boolean {
                if (textView.text.toString().isBlank()) return false
                if (i == EditorInfo.IME_ACTION_GO || i == EditorInfo.IME_ACTION_SEARCH) {
                    /*隐藏软键盘*/
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    // 隐藏软键盘
                    imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                    setSearchText(getSearchText())
                    return true
                }
                return false
            }
        })
        pages = buildPages()
        pagerAdapter = SearchPagerAdapter(supportFragmentManager, this, pages)
        binding.pager.adapter = pagerAdapter
        binding.tabs.setupWithViewPager(binding.pager)
    }


    @Suppress("DEPRECATION")
    private fun popUpKeyboard() {
        binding.searchview.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun getSearchText(): String {
        return binding.searchview.text.toString()
    }

    fun setSearchText(text: String) {
        for (f in supportFragmentManager.fragments) {
            when (f) {
                is RuntimeSearchTabFragment -> f.setQuery(text)
                is FragmentSearchResult -> f.setSearchText(text)
            }
        }
    }

    /** 搜索 tab 来自插件贡献点；Fragment 与 View 两种形态统一成宿主页面。 */
    private fun buildPages(): List<SearchPage> {
        val host = (application as HApplication).pluginHost
        val fragmentPages = host.searchTabs.entries().map { tab ->
            SearchPage(
                id = tab.id,
                title = tab.textTitle ?: getString(tab.titleRes),
                order = tab.order,
                create = tab.create,
            )
        }
        val viewPages = host.searchTabs.viewEntries().map { tab ->
            SearchPage(
                id = tab.id,
                title = tab.title,
                order = tab.order,
                create = { RuntimeSearchTabFragment.newInstance(tab.id) },
            )
        }
        return (fragmentPages + viewPages).sortedBy { it.order }
    }

    private class SearchPagerAdapter(
        fm: FragmentManager,
        private val context: Context,
        private val pages: List<SearchPage>,
    ) : BaseTabAdapter(fm, pages.size) {

        override fun getPageTitle(position: Int): CharSequence = pages[position].title

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            mFragments[position] = null
        }

        override fun initItem(position: Int): Fragment = pages[position].create()
    }

    override fun initViewBinding(): ActivitySearchBinding {
        return ActivitySearchBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<SearchViewModel> {
        return SearchViewModel::class.java
    }

}
