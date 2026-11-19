package com.stupidtree.hita.api

import kotlin.jvm.internal.DefaultConstructorMarker

/**
 * 插件贡献工具时使用的纯 DTO。`:api` 不能引用 `:app` 的 ToolSpec，
 * 这里只描述 schema 与字符串处理函数，由宿主适配到内部工具注册表。
 */
data class PluginToolParam(
    val name: String,
    /** string / integer / number / boolean */
    val type: String = "string",
    val required: Boolean = false,
    val description: String = "",
    val aliases: List<String> = emptyList(),
    val enumValues: List<String> = emptyList(),
)

/** 兼容旧插件常用的类型别名。 */
fun PluginToolParam.normalizedType(): String = when (type.trim().lowercase()) {
    "int", "long" -> "integer"
    "float", "double" -> "number"
    "bool" -> "boolean"
    else -> type.trim().lowercase()
}

data class PluginToolSpec(
    val name: String,
    val description: String,
    val parameters: List<PluginToolParam> = emptyList(),
    /** 结果 JSON Schema；为空表示不向模型声明固定输出形状。 */
    val outputSchemaJson: String = "",
    val maxObservationChars: Int = 1800,
    val timeoutMs: Long = 15_000,
    /** 工具失败后重试不会产生重复副作用。 */
    val retrySafe: Boolean = false,
    /** 允许模型把单个必填 string 参数直接写成原文，而不是 JSON 对象。 */
    val allowRawString: Boolean = false,
) {
    /** 保留 API 8 插件已经编译进 dex 的构造器签名。 */
    constructor(
        name: String,
        description: String,
        parameters: List<PluginToolParam>,
        maxObservationChars: Int,
        timeoutMs: Long,
    ) : this(
        name = name,
        description = description,
        parameters = parameters,
        outputSchemaJson = "",
        maxObservationChars = maxObservationChars,
        timeoutMs = timeoutMs,
        retrySafe = false,
        allowRawString = false,
    )

    /**
     * 保留 API 8 编译器生成的默认参数构造器签名。
     *
     * 旧插件即使只写 name/description，也会调用带 mask 的 synthetic 构造器；只保留
     * 5 参显式构造器不足以避免 `NoSuchMethodError`。
     */
    @Suppress("UNUSED_PARAMETER")
    constructor(
        name: String,
        description: String,
        parameters: List<PluginToolParam>?,
        maxObservationChars: Int,
        timeoutMs: Long,
        mask: Int,
        marker: DefaultConstructorMarker?,
    ) : this(
        name = name,
        description = description,
        parameters = if (mask and 4 != 0) emptyList() else parameters.orEmpty(),
        outputSchemaJson = "",
        maxObservationChars = if (mask and 8 != 0) 1800 else maxObservationChars,
        timeoutMs = if (mask and 16 != 0) 15_000L else timeoutMs,
        retrySafe = false,
        allowRawString = false,
    )
}

/** 插件工具名与宿主工具名共用的规范化规则。 */
fun normalizeToolName(name: String): String =
    name.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

object PluginToolSchema {
    private val TYPES = setOf("string", "integer", "number", "boolean")

    /** 返回第一条元数据错误；合法时返回 null。 */
    fun validationError(spec: PluginToolSpec): String? {
        if (normalizeToolName(spec.name).isBlank()) return "工具名不能为空，且只能包含字母、数字和下划线"
        if (spec.description.isBlank()) return "工具描述不能为空"
        if (spec.maxObservationChars <= 0) return "maxObservationChars 必须大于 0"
        if (spec.timeoutMs <= 0) return "timeoutMs 必须大于 0"
        val seen = mutableSetOf<String>()
        spec.parameters.forEach { param ->
            val normalized = normalizeToolName(param.name)
            if (normalized.isBlank()) return "参数名不能为空：${param.name}"
            if (!seen.add(normalized)) return "参数名重复：${param.name}"
            if (param.normalizedType() !in TYPES) return "参数 ${param.name} 类型不支持：${param.type}"
        }
        return null
    }
}

fun interface PluginToolHandler {
    /** inputJson 是模型给出的动作输入，返回观察文本；抛异常会被宿主转成工具错误。 */
    fun execute(inputJson: String): String?
}

interface PluginToolRegistry {
    fun register(spec: PluginToolSpec, handler: PluginToolHandler): AutoCloseable
}
