package com.stupidtree.hitax.agent.tools

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReActToolRegistryTest {

    private val registry = ReActToolRegistry.createDefault()

    private fun validate(name: String, input: String): List<String> =
        registry.validate(registry.spec(name)!!, input)

    @Test
    fun catalogContainsEveryRegisteredToolAndRequiredFields() {
        val lines = registry.catalog().lines().filter { it.startsWith("- ") }

        assertEquals(14, lines.size)
        assertTrue(lines.any { it.contains("submit_review") && it.contains("必填：course_code,content") })
        assertTrue(lines.any { it.contains("get_course_detail") && it.contains("course_code") })
        assertTrue(lines.any { it.contains("web_search") && it.contains("param") })
        assertTrue(lines.any { it.contains("day_offset=相对今天的天数") })
        assertTrue(lines.any { it.contains("week=周次") })
    }

    @Test
    fun requiredFieldIsEnforcedForJsonArguments() {
        assertEquals(listOf("缺少参数 name"), validate("search_teacher", "{}"))
        assertEquals(listOf("缺少参数 course_code", "缺少参数 content"), validate("submit_review", "{}"))
    }

    @Test
    fun aliasesStillSatisfyRequiredFields() {
        assertTrue(validate("search_teacher", """{"teacher_name":"张老师"}""").isEmpty())
        assertTrue(validate("search_timetable", """{"keyword":"高数"}""").isEmpty())
        assertTrue(validate("web_search", """{"query":"kotlin"}""").isEmpty())
    }

    @Test
    fun wrongTypeAndEnumAreRejected() {
        assertEquals(
            listOf("参数 name 应为 string"),
            validate("search_teacher", """{"name":42}"""),
        )
        assertEquals(
            listOf("参数 review_type 必须是 lecturer/course/course_teacher/section/new_course"),
            validate(
                "submit_review",
                """{"course_code":"CS1","content":"ok","review_type":"unknown"}""",
            ),
        )
    }

    @Test
    fun nonJsonLegacyInputIsLeftToTheTool() {
        assertTrue(validate("search_teacher", "张老师").isEmpty())
        assertTrue(validate("get_timetable", "").isEmpty())
        assertTrue(validate("get_timetable", "今天").isEmpty())
    }

    @Test
    fun nonJsonInputIsRejectedForMultiFieldTools() {
        assertEquals(
            listOf("动作输入必须是 JSON 对象"),
            validate("add_activity", "明天开会"),
        )
    }

    @Test
    fun blankRequiredStringIsRejected() {
        assertEquals(
            listOf("参数 name 不能为空"),
            validate("search_teacher", """{"name":""}"""),
        )
    }

    @Test
    fun catalogEnumPlaceholderIsValidJsonAndAliasesAreVisible() {
        val catalog = registry.catalog()
        assertTrue(catalog.contains("\"review_type\":\"lecturer\""))
        assertFalse(catalog.contains("lecturer|course"))
        assertTrue(catalog.contains("review_type 可选值：lecturer,course"))
        assertTrue(catalog.contains("teacher_name→name"))
    }

    @Test
    fun failingPostProcessorFailsClosed() = runBlocking {
        val registry = ReActToolRegistry()
        registry.register(
            ToolSpec(
                name = "x",
                description = "x",
                tool = SuspendingReActTool { "secret" },
            ),
        )
        registry.postProcess { _, _ -> error("redaction failed") }

        val result = registry.runPipeline(
            registry.spec("x")!!,
            ToolCall("x", ""),
        ) { "secret" }

        assertTrue(result.error)
        assertEquals("工具结果后处理失败", result.text)
    }

    @Test
    fun guardDenySkipsTheToolBody() = runBlocking {
        val registry = ReActToolRegistry()
        registry.register(
            ToolSpec(
                name = "x",
                description = "x",
                tool = SuspendingReActTool { "ok" },
            ),
        )
        registry.guard { ToolDecision.Deny("blocked") }
        var called = false

        val result = registry.runPipeline(
            registry.spec("x")!!,
            ToolCall("x", ""),
        ) {
            called = true
            "ok"
        }

        assertTrue(result.error)
        assertTrue(result.text.contains("blocked"))
        assertFalse(called)
    }

    @Test
    fun postProcessorAndPerToolTruncationRunInOrder() = runBlocking {
        val registry = ReActToolRegistry()
        registry.register(
            ToolSpec(
                name = "x",
                description = "x",
                maxObservationChars = 10,
                tool = SuspendingReActTool { "ok" },
            ),
        )
        registry.postProcess { _, result -> result.copy(text = result.text + "!") }

        val result = registry.runPipeline(
            registry.spec("x")!!,
            ToolCall("x", ""),
        ) { "abcdefghijklm" }

        assertTrue(result.truncated)
        assertTrue(result.text.endsWith("…[truncated]"))
        assertTrue(result.text.startsWith("abcdefghij"))
    }

    @Test
    fun retrySafeToolIsRetriedOnce() = runBlocking {
        val registry = ReActToolRegistry()
        registry.register(
            ToolSpec(
                name = "flaky",
                description = "flaky",
                retrySafe = true,
                tool = SuspendingReActTool { "ok" },
            ),
        )
        var calls = 0

        val result = registry.runPipeline(
            registry.spec("flaky")!!,
            ToolCall("flaky", ""),
        ) {
            calls++
            if (calls == 1) error("first failure") else "ok"
        }

        assertEquals(2, calls)
        assertFalse(result.error)
        assertEquals("ok", result.text)
    }

    @Test
    fun timeoutBecomesAStructuredToolError() = runBlocking {
        val registry = ReActToolRegistry()
        registry.register(
            ToolSpec(
                name = "slow",
                description = "slow",
                timeoutMs = 1,
                tool = SuspendingReActTool { "ok" },
            ),
        )

        val result = registry.runPipeline(
            registry.spec("slow")!!,
            ToolCall("slow", ""),
        ) {
            delay(100)
            "late"
        }

        assertTrue(result.error)
        assertTrue(result.text.contains("超时"))
    }

    @Test
    fun invalidArgumentsPreventToolInvocation() = runBlocking {
        val registry = ReActToolRegistry()
        registry.register(
            ToolSpec(
                name = "need",
                description = "need",
                parameters = listOf(ToolParam("name", required = true)),
                tool = SuspendingReActTool { "ok" },
            ),
        )
        var called = false

        val result = registry.runPipeline(
            registry.spec("need")!!,
            ToolCall("need", "{}"),
        ) {
            called = true
            "ok"
        }

        assertTrue(result.error)
        assertFalse(called)
    }

    @Test
    fun codeFenceIsStrippedBeforeValidation() {
        val wrapped = "```json\n{\"name\":\"张老师\"}\n```"
        assertTrue(validate("search_teacher", wrapped).isEmpty())
    }

    @Test
    fun prepareCallParsesOnceAndCanonicalizesAliases() {
        val spec = registry.spec("search_teacher")!!
        val prepared = registry.prepareCall(spec, """{"teacher_name":"张老师"}""") as PreparedCall.Valid

        assertEquals("张老师", prepared.call.arguments.get("name").asString)
        assertEquals("张老师", prepared.call.arguments.get("teacher_name").asString)
        assertEquals(ReActToolRegistry.normalizeName("search_teacher"), prepared.call.name)
    }

    @Test
    fun multiFieldToolKeepsTypedDefaults() {
        val prepared = registry.prepareCall(
            registry.spec("add_activity")!!,
            """{"name":"组会","day_offset":1,"start_time":"15:00","end_time":"16:00"}""",
        ) as PreparedCall.Valid

        assertEquals(1, prepared.call.arguments.get("day_offset").asInt)
        assertEquals("15:00", prepared.call.arguments.get("start_time").asString)
        assertEquals(
            "组会",
            AddActivityTool().parseAddActivityInput(prepared.call.arguments).name,
        )
    }

    @Test
    fun everyMigratedCategoryRejectsMissingRequiredArguments() {
        assertEquals(listOf("缺少参数 name"), validate("search_teacher", "{}"))
        assertEquals(listOf("缺少参数 url"), validate("crawl_page", "{}"))
        assertEquals(listOf("缺少参数 url"), validate("crawl_site", "{}"))
        assertEquals(listOf("缺少参数 task_id"), validate("crawl_status", "{}"))
        assertEquals(listOf("缺少参数 query"), validate("search_course", "{}"))
        assertEquals(listOf("缺少参数 query"), validate("search_external_resource", "{}"))
        assertEquals(listOf("缺少参数 query"), validate("rag_search", "{}"))
        assertEquals(listOf("缺少参数 param"), validate("web_search", "{}"))
        assertEquals(listOf("缺少参数 name"), validate("add_activity", "{}"))
    }

    @Test
    fun zeroParameterToolAcceptsRawInputAndLeavesArgumentsEmpty() {
        val prepared = registry.prepareCall(
            registry.spec("get_timetable")!!,
            "今天",
        ) as PreparedCall.Valid

        assertEquals("今天", prepared.call.actionInput)
        assertEquals(0, prepared.call.arguments.size())
    }

    @Test
    fun rawStringToolsMapTheSingleRequiredStringIntoArguments() {
        val teacher = registry.prepareCall(
            registry.spec("search_teacher")!!,
            "张老师",
        ) as PreparedCall.Valid
        assertEquals("张老师", teacher.call.arguments.get("name").asString)

        val course = registry.prepareCall(
            registry.spec("search_course")!!,
            "数据结构",
        ) as PreparedCall.Valid
        assertEquals("数据结构", course.call.arguments.get("query").asString)
    }

    @Test
    fun multiFieldToolStillRejectsRawStringInput() {
        val prepared = registry.prepareCall(registry.spec("add_activity")!!, "明天开会")
        assertTrue(prepared is PreparedCall.Invalid)
        assertEquals("动作输入必须是 JSON 对象", (prepared as PreparedCall.Invalid).message)
    }

    @Test
    fun migratedToolParsersReadArgumentsInsteadOfRawInput() {
        val arguments = com.google.gson.JsonParser.parseString(
            """{"course_code":"CS101","start":10,"limit":20,"query":"栈"}""",
        ).asJsonObject

        val detail = GetCourseDetailTool()
        assertEquals("CS101", detail.extractCourseCode(arguments))
        assertEquals("栈", detail.extractFocusQuery(arguments))
        assertEquals(GetCourseDetailTool.TextRange(10, 30), detail.extractCharRange(arguments))

        val pageRange = CrawlPageTool().extractCharRange(
            com.google.gson.JsonParser.parseString("""{"start":5,"char_end":9}""").asJsonObject,
        )
        assertEquals(CrawlPageTool.TextRange(5, 9), pageRange)

        val classroom = SearchEmptyClassroomTool().parseParams(
            com.google.gson.JsonParser.parseString("""{"week":3,"building":"A","day":9}""").asJsonObject,
        )
        assertEquals(3, classroom.week)
        assertEquals("A", classroom.building)
        assertNull(classroom.day)

        val teacher = com.google.gson.JsonParser.parseString("""{"teacher_name":"李老师"}""").asJsonObject
        assertEquals("李老师", SearchTeacherTool().extractQuery(teacher))
    }

    @Test
    fun jsonSchemaMatchesCatalogContract() {
        val teacher = com.google.gson.JsonParser.parseString(registry.jsonSchema("search_teacher")!!).asJsonObject
        val teacherParams = teacher.getAsJsonObject("parameters")
        assertEquals("object", teacherParams.get("type").asString)
        assertEquals("name", teacherParams.getAsJsonArray("required").single().asString)
        assertTrue(teacherParams.getAsJsonObject("properties").getAsJsonObject("name").get("aliases").asJsonArray
            .any { it.asString == "teacher_name" })

        val review = com.google.gson.JsonParser.parseString(registry.jsonSchema("submit_review")!!).asJsonObject
        val reviewType = review.getAsJsonObject("parameters")
            .getAsJsonObject("properties")
            .getAsJsonObject("review_type")
        assertEquals(5, reviewType.getAsJsonArray("enum").size())
    }

    @Test
    fun submitReviewMissingFieldsFailsBeforeSideEffects() {
        assertEquals(listOf("缺少参数 course_code", "缺少参数 content"), validate("submit_review", "{}"))
        assertEquals(
            listOf("参数 review_type 必须是 lecturer/course/course_teacher/section/new_course"),
            validate("submit_review", """{"course_code":"CS1","content":"ok","review_type":"unknown"}"""),
        )
    }
}
