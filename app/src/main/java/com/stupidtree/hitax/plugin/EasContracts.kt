package com.stupidtree.hitax.plugin

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.EasSessionService
import com.stupidtree.hita.api.EasUiService
import com.stupidtree.hita.api.dto.EasAccountDto
import com.stupidtree.hita.api.dto.EasImportOptionsDto
import com.stupidtree.hita.api.dto.EasLoginRequestDto
import com.stupidtree.hita.api.dto.EasLoginResultDto
import com.stupidtree.hita.api.dto.EasReimportResultDto
import com.stupidtree.hita.kernel.PluginHost
import com.stupidtree.hitax.HApplication
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 宿主读取 EAS 能力的唯一入口。
 *
 * 优先新 key；插件是旧版本时回落到旧 `EasService` 适配器。
 * 适配器只持有服务引用，绝不持有 Activity。
 */
object EasContracts {

    fun account(context: Context): EasAccountDto =
        accountService(context)?.account() ?: EasAccountDto()

    /** 优先 `EasAccountService`，否则从旧 `EasService` 映射。 */
    fun accountService(context: Context): EasAccountService? {
        val host = host(context) ?: return null
        host.service(EasAccountService.KEY)?.let { return it }

        val legacy = host.service(EasService.KEY) ?: return null
        val auth = host.service(EasAuthService.KEY)
        return LegacyEasAccountService(legacy) { auth?.hasApiToken() == true }
    }

    fun session(context: Context): EasSessionService? {
        val host = host(context) ?: return null
        host.service(EasSessionService.KEY)?.let { return it }
        return host.service(EasService.KEY)?.let(::LegacyEasSessionService)
    }

    fun ui(context: Context): EasUiService? {
        val host = host(context) ?: return null
        host.service(EasUiService.KEY)?.let { return it }

        val legacy = host.service(EasService.KEY) ?: return null
        val account = accountService(context) ?: return null
        return LegacyEasUiService(legacy, account)
    }

    private fun host(context: Context): PluginHost? =
        (context.applicationContext as? HApplication)?.pluginHost
}

/**
 * 旧 `EasService` 的账号快照适配。
 *
 * `hasApiToken` 无法从旧接口得知，因此优先问 `EasAuthService`；没有时保守为 false。
 * `subtitle` 的空串按新 DTO 约定折成 null。
 */
internal class LegacyEasAccountService(
    private val legacy: EasService,
    private val hasApiToken: () -> Boolean,
) : EasAccountService {
    override fun account(): EasAccountDto {
        if (!legacy.isLogin()) return EasAccountDto(loggedIn = false)
        return EasAccountDto(
            loggedIn = true,
            hasApiToken = hasApiToken(),
            displayName = legacy.accountName(),
            subtitle = legacy.accountSubtitle()?.takeIf { it.isNotBlank() },
            studentId = legacy.currentStuId(),
            avatarUrl = legacy.accountAvatarUrl(),
        )
    }
}

/** 旧 `EasService` 的会话命令适配；重复回调由 gate 去重。 */
internal class LegacyEasSessionService(
    private val legacy: EasService,
) : EasSessionService {
    override fun logout() = legacy.logout()

    override fun autoReimportIfDue() = legacy.autoReimportIfDue()

    override fun startAutoReimport(listener: (EasReimportResultDto) -> Unit) {
        val finished = AtomicBoolean(false)
        legacy.startAutoReimport { success ->
            if (finished.compareAndSet(false, true)) {
                listener(
                    EasReimportResultDto(
                        success = success,
                        errorCode = if (success) null else "IMPORT_FAILED",
                        finishedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }
}

/**
 * 旧 `EasService` 的页面入口适配。
 *
 * 方法只接收 Activity 参数，不在字段里保存 Activity；取消登录仍不回调。
 */
internal class LegacyEasUiService(
    private val legacy: EasService,
    private val accountService: EasAccountService,
) : EasUiService {

    override fun ensureLogin(
        activity: FragmentActivity,
        request: EasLoginRequestDto,
        callback: (EasLoginResultDto) -> Unit,
    ) {
        val finished = AtomicBoolean(false)
        legacy.ensureLogin(
            activity,
            onSuccess = {
                if (finished.compareAndSet(false, true)) {
                    callback(
                        EasLoginResultDto(
                            success = true,
                            account = accountService.account(),
                        )
                    )
                }
            },
            onFailed = {
                if (finished.compareAndSet(false, true)) {
                    callback(
                        EasLoginResultDto(
                            success = false,
                            errorCode = "LOGIN_FAILED",
                        )
                    )
                }
            },
        )
    }

    override fun openAccount(activity: FragmentActivity) = legacy.openAccount(activity)

    override fun openImportTimetable(
        activity: FragmentActivity,
        options: EasImportOptionsDto,
    ) = legacy.openImportTimetable(activity, options.autoImport)

    override fun openScores(activity: FragmentActivity) = legacy.openScores(activity)

    override fun openClassroom(activity: FragmentActivity) = legacy.openClassroom(activity)

    override fun openExam(activity: FragmentActivity) = legacy.openExam(activity)
}
