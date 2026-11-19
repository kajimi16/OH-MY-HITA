package com.stupidtree.hitax.agent.tools

import com.stupidtree.hitax.agent.remote.CourseContributionOps
import com.stupidtree.hitax.agent.remote.PrServerClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubmitReviewTool : ReActTool {
    override fun execute(input: ReActToolInput): String {
        val courseCode = input.arguments.stringArg("course_code").orEmpty()
        val content = input.arguments.stringArg("content").orEmpty()
        val authorName = input.arguments.stringArg("author_name").orEmpty().ifBlank { "匿名" }
        val lecturerName = input.arguments.stringArg("lecturer_name").orEmpty()
        val teacherName = input.arguments.stringArg("teacher_name").orEmpty()
        val courseName = input.arguments.stringArg("course_name").orEmpty()
        val reviewType = input.arguments.stringArg("review_type").orEmpty().ifBlank { "lecturer" }
        val title = input.arguments.stringArg("title").orEmpty().ifBlank { "课程评价" }

        if (courseCode.isBlank() || content.isBlank()) return "提交评价失败：缺少课程代码或评价内容"

        val author = mapOf(
            "name" to authorName,
            "link" to "",
            "date" to SimpleDateFormat("yyyy-MM", Locale.CHINA).format(Date()),
        )
        val ops = when (reviewType) {
            "course", "new_course" -> {
                if (courseName.isBlank()) return "提交评价失败：多项目课程评价需要 course_name"
                CourseContributionOps.courseSection(
                    courseName = courseName,
                    sectionTitle = title,
                    content = content,
                    author = author,
                    createCourse = reviewType == "new_course",
                )
            }
            "course_teacher" -> {
                if (courseName.isBlank() || teacherName.isBlank()) return "提交评价失败：课程教师评价需要 course_name 和 teacher_name"
                CourseContributionOps.courseTeacherReview(
                    courseName = courseName,
                    teacherName = teacherName,
                    content = content,
                    author = author,
                    createCourse = false,
                )
            }
            "section" -> {
                if (title.isBlank()) return "提交评价失败：章节内容需要 title"
                listOf(mapOf("op" to "add_section_item", "title" to title, "content" to content, "author" to author))
            }
            else -> {
                if (lecturerName.isBlank()) return "提交评价失败：缺少教师姓名（lecturer_name）"
                listOf(mapOf("op" to "add_lecturer_review", "lecturer_name" to lecturerName, "content" to content, "author" to author))
            }
        }

        val preview = PrServerClient.previewSync(courseCode, ops)
        if (!preview.ok) return "评价预览失败：${preview.error?.message ?: "未知错误"}"
        val result = PrServerClient.submitSync(courseCode, ops)
        if (result.ok) {
            val url = result.data?.pr?.url
            return if (!url.isNullOrBlank()) "评价提交成功！PR 链接：$url" else "评价提交成功"
        }
        return "评价提交失败：${result.error?.message ?: "未知错误"}"
    }
}
