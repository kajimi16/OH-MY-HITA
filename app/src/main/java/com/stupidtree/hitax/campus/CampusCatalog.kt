package com.stupidtree.hitax.campus

/**
 * 宿主内部校区目录。
 *
 * 只服务宿主 UI 的登录入口与能力门控；不进入 `:api` / `:model`，插件不依赖它。
 * 前置条件：无。
 * 后置条件：目录内容与顺序稳定；[byId] 对未知 id 返回 null，不抛异常。
 * 不变式：每个 id 唯一且非空；[default] 永远在 [all] 中。
 */
enum class CampusAuthMode { API, WEB }

/**
 * 一个校区及其可用认证方式。
 *
 * [unavailableNote] 仅在缺少 API 登录时非空，用于登录对话框提示；文案与旧 UI 完全一致。
 */
data class CampusProvider(
    val id: String,
    val displayName: String,
    val authModes: Set<CampusAuthMode>,
    val unavailableNote: String? = null,
) {
    val supportsApiLogin: Boolean get() = CampusAuthMode.API in authModes
    val supportsWebLogin: Boolean get() = CampusAuthMode.WEB in authModes
}

object CampusCatalog {
    const val SHENZHEN = "hit.shenzhen"
    const val HARBIN = "hit.harbin"
    const val WEIHAI = "hit.weihai"

    /** 非深圳校区的统一说明；与 0.10.x 登录弹层文案保持一致。 */
    const val API_UNAVAILABLE_NOTE = "当前宿主仅支持深圳 API 登录，请使用网页登录"

    /** 展示顺序即登录对话框中的顺序。 */
    val all: List<CampusProvider> = listOf(
        CampusProvider(
            id = SHENZHEN,
            displayName = "深圳",
            authModes = setOf(CampusAuthMode.API, CampusAuthMode.WEB),
        ),
        CampusProvider(
            id = HARBIN,
            displayName = "本部",
            authModes = setOf(CampusAuthMode.WEB),
            unavailableNote = API_UNAVAILABLE_NOTE,
        ),
        CampusProvider(
            id = WEIHAI,
            displayName = "威海",
            authModes = setOf(CampusAuthMode.WEB),
            unavailableNote = API_UNAVAILABLE_NOTE,
        ),
    )

    private val byIdMap: Map<String, CampusProvider> = all.associateBy { it.id }

    val default: CampusProvider = requireNotNull(byIdMap[SHENZHEN]) {
        "CampusCatalog 缺少默认校区 $SHENZHEN"
    }

    /**
     * 按 id 查询校区。
     *
     * 前置条件：无。
     * 后置条件：命中返回目录条目；未知 id 或 null 返回 null。
     */
    fun byId(id: String?): CampusProvider? = id?.let(byIdMap::get)

    /** 支持 API 登录的校区；当前只有深圳，非空。 */
    fun apiLoginProviders(): List<CampusProvider> = all.filter { it.supportsApiLogin }
}
