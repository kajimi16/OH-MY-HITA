package com.stupidtree.hita.theta

import com.stupidtree.hita.api.HostUiService

/** θ 插件内访问宿主能力的运行时入口。 */
object ThetaRuntime {
    @Volatile
    var hostUi: HostUiService? = null
}
