package com.stupidtree.hitax.eas

import android.app.Application
import com.stupidtree.hita.api.EasSessionService
import com.stupidtree.hita.api.dto.EasReimportResultDto
import com.stupidtree.hitax.data.model.eas.EASToken
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.repository.EasSettingsRepository
import java.util.concurrent.atomic.AtomicBoolean

/** 会话命令需要的最小依赖面，便于纯 JVM 测试。 */
internal interface EasSessionGateway {
    fun isLoggedIn(): Boolean
    fun isUndergraduate(): Boolean
    fun logout()
    fun autoReimportEnabled(): Boolean
    fun lastAutoReimportAt(): Long
    fun setLastAutoReimportAt(ts: Long)

    /** 实现必须回调一次；本类仍会用 gate 防止重复。 */
    fun startImportCurrentTimetable(isUndergraduate: Boolean, onResult: (Boolean) -> Unit)
}

internal class AndroidEasSessionGateway(
    private val application: Application,
) : EasSessionGateway {
    private val repo get() = EASRepository.getInstance(application)
    private val settings get() = EasSettingsRepository.getInstance(application)
    private val token: EASToken get() = repo.getEasToken()

    override fun isLoggedIn(): Boolean = token.isLogin()
    override fun isUndergraduate(): Boolean = token.stutype == EASToken.TYPE.UNDERGRAD
    override fun logout() = repo.logout()
    override fun autoReimportEnabled(): Boolean = settings.isAutoReimportEnabled()
    override fun lastAutoReimportAt(): Long = settings.getLastAutoReimportTs()
    override fun setLastAutoReimportAt(ts: Long) = settings.setLastAutoReimportTs(ts)

    override fun startImportCurrentTimetable(
        isUndergraduate: Boolean,
        onResult: (Boolean) -> Unit,
    ) = repo.startAutoImportCurrentTimetable(isUndergraduate, onResult)
}

/**
 * 会话命令实现。`startAutoReimport` 用本地 gate 保证 listener 恰好一次，
 * 即使底层仓库回调重复或抛异常。
 */
internal class EasSessionServiceImpl(
    private val gateway: EasSessionGateway,
) : EasSessionService {

    constructor(application: Application) : this(AndroidEasSessionGateway(application))

    override fun logout() = gateway.logout()

    override fun autoReimportIfDue() {
        if (!gateway.autoReimportEnabled()) return
        if (!gateway.isLoggedIn()) return
        val now = System.currentTimeMillis()
        if (now - gateway.lastAutoReimportAt() < INTERVAL_MS) return
        startAutoReimport { }
    }

    override fun startAutoReimport(listener: (EasReimportResultDto) -> Unit) {
        val finished = AtomicBoolean(false)
        fun finish(result: EasReimportResultDto) {
            if (finished.compareAndSet(false, true)) listener(result)
        }

        if (!gateway.isLoggedIn()) {
            finish(
                EasReimportResultDto(
                    success = false,
                    errorCode = "NOT_LOGGED_IN",
                    finishedAt = System.currentTimeMillis(),
                )
            )
            return
        }

        try {
            gateway.startImportCurrentTimetable(gateway.isUndergraduate()) { success ->
                // 先抢占 gate，再写时间戳，避免重复回调把 lastTs 写两次。
                if (!finished.compareAndSet(false, true)) return@startImportCurrentTimetable
                if (success) gateway.setLastAutoReimportAt(System.currentTimeMillis())
                listener(
                    EasReimportResultDto(
                        success = success,
                        errorCode = if (success) null else "IMPORT_FAILED",
                        finishedAt = System.currentTimeMillis(),
                    )
                )
            }
        } catch (t: Throwable) {
            finish(
                EasReimportResultDto(
                    success = false,
                    errorCode = "IMPORT_ERROR",
                    message = t.message,
                    finishedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    companion object {
        private const val INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}
