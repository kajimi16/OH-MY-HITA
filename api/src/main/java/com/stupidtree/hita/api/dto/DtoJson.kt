package com.stupidtree.hita.api.dto

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * DTO 的 JSON 编解码入口。
 *
 * - 固定开启 `serializeNulls`，保证字段出现且为 `null`，避免“缺字段”和“空值”混淆。
 * - 不注册多态 adapter；DTO 只含具体类型。
 * - `ByteArray` 使用 Gson 默认编码（JSON number array）。不使用 `android.util.Base64`，
 *   也不依赖 API 26 的 `java.util.Base64`，保证 minSdk 23 可用。
 */
object DtoJson {
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .create()

    fun toJson(value: Any): String = gson.toJson(value)

    fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)
}
