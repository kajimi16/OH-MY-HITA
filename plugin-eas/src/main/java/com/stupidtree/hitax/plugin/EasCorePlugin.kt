package com.stupidtree.hitax.plugin

import android.app.Application
import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.HostEnvironmentService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.eas.EasPlugin

/**
 * T1 EAS entry. Legacy EAS services are registered as-is; no Activity is
 * started. The auth contract is headless so a T2 Compose page can log in.
 */
class EasCorePlugin : Plugin {
    override val id = "com.stupidtree.hitax.eas"
    override val displayName = "教务核心"
    override val requiresApi = 11

    override fun install(ctx: PluginContext) {
        val app = (ctx.service(HostEnvironmentService.KEY)?.applicationContext as? Application)
            ?: error("缺少宿主 applicationContext")
        EasPlugin(app).install(ctx)
        ctx.provide(EasAuthService.KEY, EasAuthImpl(app))
    }

    override fun uninstall(ctx: PluginContext) {
        // 服务注册由内核统一回滚；这里没有额外资源需要手动释放。
    }

    override fun purgeData() = Unit
}

private class EasAuthImpl(private val application: Application) : EasAuthService {
    private val repo get() = EASRepository.getInstance(application)

    override fun isLogin(): Boolean = repo.getEasToken().isLogin()
    override fun accountName(): String? = repo.getEasToken().name?.ifBlank { repo.getEasToken().username }
    override fun accountSubtitle(): String? = repo.getEasToken().stuId?.ifBlank { repo.getEasToken().username }

    override fun login(username: String, password: String): LiveData<DataState<Boolean>> =
        repo.login(username, password)

    override fun loginWithWebSession(
        cookies: Map<String, String>,
        webBaseUrl: String?,
    ): LiveData<DataState<Boolean>> = repo.loginWithWebSession(cookies, webBaseUrl)

    override fun logout() = repo.logout()
}
