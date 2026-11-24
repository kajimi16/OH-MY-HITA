package com.stupidtree.hitax.agent

import android.content.Context
import com.stupidtree.hitax.market.PresetStateStore

/**
 * 写操作确认是 HITA_Android 3.3.x 的行为。
 *
 * 旧 preset（3.1.2 及更早）保持原行为，避免把新交互强加给已有用户。
 */
object AgentWriteConfirmationPolicy {
    private const val ANDROID_33_PREFIX = "hita-android-3.3."

    fun isEnabled(context: Context): Boolean =
        isEnabledForPreset(PresetStateStore.activeId(context))

    fun isEnabledForPreset(presetId: String?): Boolean =
        presetId?.startsWith(ANDROID_33_PREFIX) == true
}
