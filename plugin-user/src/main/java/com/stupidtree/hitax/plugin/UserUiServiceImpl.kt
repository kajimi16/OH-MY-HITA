package com.stupidtree.hitax.plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import com.stupidtree.hita.api.UserUiService
import com.stupidtree.hitax.ui.profile.ProfileActivity

class UserUiServiceImpl(@Suppress("UNUSED_PARAMETER") private val application: android.app.Application) :
    UserUiService {

    override fun openProfile(context: Context, userId: String?, avatar: View?) {
        val intent = Intent(context, ProfileActivity::class.java).putExtra("id", userId)
        if (avatar != null && context is Activity) {
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                context, avatar, "useravatar"
            )
            context.startActivity(intent, options.toBundle())
        } else {
            context.startActivity(intent)
        }
    }
}
