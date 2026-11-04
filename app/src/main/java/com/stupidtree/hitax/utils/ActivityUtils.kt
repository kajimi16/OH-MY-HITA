package com.stupidtree.hitax.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.stupidtree.hitax.R
import com.stupidtree.hitax.ui.news.NewsDetailActivity
import com.stupidtree.hitax.ui.search.SearchActivity
import com.stupidtree.stupiduser.data.model.CheckUpdateResult
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.widgets.PopUpText
import com.stupidtree.style.widgets.PopUpUpdate


object ActivityUtils {


    enum class SearchType { TEACHER, CLASS,ARTICLE,USER }

    fun searchFor(from: Context, text: String?, type: SearchType) {
        if (text.isNullOrBlank()) return
        val i = Intent(from, SearchActivity::class.java)
        i.putExtra("keyword", text)
        i.putExtra("type", type.name)
        from.startActivity(i)

    }

    fun startSearchActivity(from: Context) {
        val i = Intent(from, SearchActivity::class.java)
        from.startActivity(i)
    }

    fun startSearchActivity(from: Activity,transition: View) {
        val i = Intent(from, SearchActivity::class.java)
        transition.transitionName = "search"
        val ao = ActivityOptionsCompat.makeSceneTransitionAnimation(from,transition,"search")
        from.startActivity(i,ao.toBundle())
    }
    fun startImportTimetableActivity(from: Context, autoImport: Boolean = false) {
        (from as? androidx.fragment.app.FragmentActivity)?.let { activity ->
            (activity.application as com.stupidtree.hitax.HApplication).pluginHost
                .service(com.stupidtree.hita.api.EasService.KEY)
                ?.openImportTimetable(activity, autoImport)
        }
    }

    fun startWelcomeActivity(from: Context) {
        if (from is BaseActivity<*, *>) {
            (from.application as com.stupidtree.hitax.HApplication).pluginHost
                .service(com.stupidtree.hita.api.EasService.KEY)
                ?.openAccount(from)
        } else {
            Toast.makeText(from, R.string.eas_login_prompt, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 进行教务认证，或直接跳转
     * @param directTo 若存在已登录token，则直接跳转到activity。传null表示忽略
     * @param lock 是否锁定窗口（=true时，若cancel则连带宿主一起销毁）
     * @param onResponseListener 认证监听
     */

    fun <T : Activity> startActivity(from: Context, activity: Class<T>) {
        val i = Intent(from, activity)
        from.startActivity(i)
    }

    fun startTimetableManager(from: Context) {
        (from.applicationContext as com.stupidtree.hitax.HApplication).pluginHost
            .service(com.stupidtree.hita.api.TimetableUiService.KEY)
            ?.openManager(from)
    }


    fun startSubjectActivity(from: Context, id: String) {
        (from.applicationContext as com.stupidtree.hitax.HApplication).pluginHost
            .service(com.stupidtree.hita.api.TimetableUiService.KEY)
            ?.openSubject(from, id)
    }

    fun startTimetableDetailActivity(from: Context, id: String) {
        (from.applicationContext as com.stupidtree.hitax.HApplication).pluginHost
            .service(com.stupidtree.hita.api.TimetableUiService.KEY)
            ?.openDetail(from, id)
    }

    fun startProfileActivity(from: Context, userId: String?, imageView: ImageView? = null) {
        (from.applicationContext as com.stupidtree.hitax.HApplication).pluginHost
            .service(com.stupidtree.hita.api.UserUiService.KEY)
            ?.openProfile(from, userId, imageView)
    }

    fun showUpdateNotificationForce(cr:CheckUpdateResult,activity: BaseActivity<*,*>){
        PopUpText().setText("版本：${cr.latestVersionName}\n更新内容：${cr.updateLog}\n" + "是否前往下载？")
            .setTitle(R.string.new_version_available)
            .setOnConfirmListener(object : PopUpText.OnConfirmListener {

                override fun OnConfirm() {
                    val uri: Uri = Uri.parse(cr.latestUrl);
                    val intent = Intent(Intent.ACTION_VIEW, uri);
                    activity.startActivity(intent)
                }
            }).show(activity.supportFragmentManager, "update")
    }


    fun showUpdateNotification(cr:CheckUpdateResult,activity: BaseActivity<*,*>){
        val preference: SharedPreferences =
            activity.application.getSharedPreferences("update_skip", Context.MODE_PRIVATE)
        val versionKey = cr.latestVersionCode.toString()
        val canceledAtKey = "canceled_at_$versionKey"
        val now = System.currentTimeMillis()
        val canceledAt = preference.getLong(canceledAtKey, 0L)
        if (!UpdatePromptPolicy.shouldPrompt(
                skipped = preference.getBoolean(versionKey, false),
                canceledAt = canceledAt,
                now = now,
            )
        ) return
        PopUpUpdate().setText("**版本：${cr.latestVersionName}**\n\n${cr.updateLog}\n\n是否前往下载？")
            .setTitle(R.string.new_version_available)
            .setOnActionListener(object : PopUpUpdate.OnActionListener {
                override fun onConfirm() {
                    val uri: Uri = Uri.parse(cr.latestUrl);
                    val intent = Intent(Intent.ACTION_VIEW, uri);
                    activity.startActivity(intent)
                }

                override fun onCancel() {
                    // 取消后一天内不再打扰，而不是下次进入应用立刻再弹。
                    preference.edit().putLong(canceledAtKey, System.currentTimeMillis()).apply()
                }

                override fun onSkip() {
                    preference.edit().putBoolean(versionKey, true).apply()
                }
            }).show(activity.supportFragmentManager, "update")
    }

    fun startNewsActivity(from: Context, url: String, title: String) {
        val i = Intent(from, NewsDetailActivity::class.java)
        i.putExtra("link", url)
        i.putExtra("title", title)
        i.putExtra("mode", "hitsz_news")
        from.startActivity(i)
    }
}
