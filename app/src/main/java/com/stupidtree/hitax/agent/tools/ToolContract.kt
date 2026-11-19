package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.stupidtree.hitax.agent.core.AgentTraceEvent
import com.stupidtree.hitax.agent.model.AgentResourceCard

enum class ToolParamType { STRING, INTEGER, NUMBER, BOOLEAN }

/** 同步工具适配到统一的可挂起执行接口。 */
fun ReActTool.asSuspending(): SuspendingReActTool = SuspendingReActTool { input -> execute(input) }

/**
 * 一个模型可见参数的声明。schema 同时用于生成工具目录与执行前校验。
 * [aliases] 让旧工具已经接受的同义字段继续通过校验。
 */
data class ToolParam(
    val name: String,
    val type: ToolParamType = ToolParamType.STRING,
    val required: Boolean = false,
    val description: String = "",
    val aliases: List<String> = emptyList(),
    val enumValues: List<String> = emptyList(),
)

data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: List<ToolParam> = emptyList(),
    /** 结果 JSON Schema；为空表示不向模型声明固定输出形状。 */
    val outputSchemaJson: String = "",
    val maxObservationChars: Int = 1800,
    /** 协作式超时：同步工具若不响应线程中断，仍可能继续运行。 */
    val timeoutMs: Long = 15_000,
    /** 工具失败后重试不会产生重复副作用。 */
    val retrySafe: Boolean = false,
    /** 允许模型把单个必填 string 参数直接写成原文，而不是 JSON 对象。 */
    val allowRawString: Boolean = false,
    val presentCall: (ToolCall) -> String = { "${it.name}(${it.actionInput.take(120)})" },
    val presentResult: (ToolExecution) -> String = { it.text.take(160) },
    val tool: SuspendingReActTool,
)

data class ToolCall(
    val name: String,
    val actionInput: String,
    val userMessage: String = "",
    /** registry 一次性解析后的参数对象；保留 [actionInput] 仅用于展示与插件兼容。 */
    val arguments: JsonObject = JsonObject(),
)

data class ToolExecution(
    val text: String,
    val error: Boolean = false,
    val truncated: Boolean = false,
    val retryable: Boolean = false,
    val cards: List<AgentResourceCard> = emptyList(),
)

sealed class ToolDecision {
    data object Allow : ToolDecision()

    data class Deny(val reason: String) : ToolDecision()

    /** 需要人类确认；在当前宿主里按拒绝处理，留给未来的审批 UI。 */
    data class Ask(val reason: String) : ToolDecision()
}

fun interface ToolGuard {
    fun check(call: ToolCall): ToolDecision
}

fun interface ToolPostProcessor {
    fun process(call: ToolCall, result: ToolExecution): ToolExecution?
}

/**
 * 从已解析参数里按顺序取第一个非空字符串。
 * [keys] 同时包含规范名与历史别名，保持旧工具接受多种字段的行为。
 */
internal fun JsonObject.stringArg(vararg keys: String): String? {
    for (key in keys) {
        val element = get(key) ?: continue
        if (element.isJsonNull || !element.isJsonPrimitive) continue
        val primitive = element.asJsonPrimitive
        if (!primitive.isString && !primitive.isNumber && !primitive.isBoolean) continue
        val value = primitive.asString.trim()
        if (value.isNotEmpty()) return value
    }
    return null
}

internal fun JsonObject.intArg(vararg keys: String): Int? {
    for (key in keys) {
        val value = numberArg(key) ?: continue
        if (value % 1.0 == 0.0 && value >= Int.MIN_VALUE && value <= Int.MAX_VALUE) {
            return value.toInt()
        }
    }
    return null
}

private fun JsonObject.numberArg(key: String): Double? {
    val element = get(key) ?: return null
    if (element.isJsonNull || !element.isJsonPrimitive) return null
    val primitive = element.asJsonPrimitive
    return when {
        primitive.isNumber -> primitive.asDouble
        primitive.isString -> primitive.asString.trim().toDoubleOrNull()
        else -> null
    }
}

/** 把别名值补到规范名上，工具只需读取规范名。 */
internal fun canonicalizeArguments(spec: ToolSpec, raw: JsonObject): JsonObject {
    val result = JsonObject()
    raw.entrySet().forEach { (key, value: JsonElement) -> result.add(key, value) }
    spec.parameters.forEach { param ->
        if (result.has(param.name)) return@forEach
        param.aliases.firstOrNull { result.has(it) }?.let { alias ->
            result.add(param.name, result.get(alias))
        }
    }
    return result
}
