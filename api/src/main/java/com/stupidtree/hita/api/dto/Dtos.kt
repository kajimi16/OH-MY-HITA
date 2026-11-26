package com.stupidtree.hita.api.dto

import com.google.gson.annotations.SerializedName

// 教务账号：纯数据快照。禁止出现 accessToken / refreshToken / password / cookies 等凭据。
data class EasAccountDto(
    @SerializedName("schemaVersion") val schemaVersion: Int = 1,
    @SerializedName("loggedIn") val loggedIn: Boolean = false,
    @SerializedName("hasApiToken") val hasApiToken: Boolean = false,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("studentId") val studentId: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("studentType") val studentType: String? = null,
    @SerializedName("school") val school: String? = null,
    @SerializedName("major") val major: String? = null,
    @SerializedName("grade") val grade: String? = null,
)

data class EasLoginRequestDto(
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("forceDialog") val forceDialog: Boolean = false,
)

data class EasLoginResultDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("cancelled") val cancelled: Boolean = false,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("account") val account: EasAccountDto? = null,
)

data class EasReimportResultDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("finishedAt") val finishedAt: Long = 0L,
)

data class EasImportOptionsDto(
    @SerializedName("autoImport") val autoImport: Boolean = false,
)

// 课表：所有时间都用 epoch millis，不使用 java.sql.Timestamp / Calendar。
data class TimetableDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("startTime") val startTime: Long = 0L,
    @SerializedName("endTime") val endTime: Long = 0L,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("periods") val periods: List<TimePeriodDto> = emptyList(),
    /** 数据来源标识；null 表示迁移前的旧行。放在末位以保持既有位置参数调用语义。 */
    @SerializedName("providerId") val providerId: String? = null,
)

data class TimePeriodDto(
    @SerializedName("fromHour") val fromHour: Int,
    @SerializedName("fromMinute") val fromMinute: Int,
    @SerializedName("toHour") val toHour: Int,
    @SerializedName("toMinute") val toMinute: Int,
)

data class EventDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("place") val place: String? = null,
    @SerializedName("teacher") val teacher: String? = null,
    @SerializedName("subjectId") val subjectId: String = "",
    @SerializedName("timetableId") val timetableId: String = "",
    @SerializedName("from") val from: Long = 0L,
    @SerializedName("to") val to: Long = 0L,
    @SerializedName("fromNumber") val fromNumber: Int = 0,
    @SerializedName("lastNumber") val lastNumber: Int = 0,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("color") val color: Int = 0,
)

data class SubjectDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String = "",
    @SerializedName("timetableId") val timetableId: String = "",
    @SerializedName("type") val type: String = "COM_A",
    @SerializedName("field") val field: String? = null,
    @SerializedName("selectCategory") val selectCategory: String? = null,
    @SerializedName("nature") val nature: String? = null,
    @SerializedName("credit") val credit: Float = 0f,
    @SerializedName("school") val school: String? = null,
    @SerializedName("countInSPA") val countInSpa: Boolean = false,
    @SerializedName("code") val code: String? = null,
    @SerializedName("key") val key: String? = null,
    @SerializedName("teacher") val teacher: String? = null,
    @SerializedName("externalCourseId") val externalCourseId: String? = null,
    @SerializedName("taskNumber") val taskNumber: String? = null,
    @SerializedName("selectionRequestId") val selectionRequestId: String? = null,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("color") val color: Int = 0,
)

data class SubjectColorDto(
    @SerializedName("id") val id: String,
    @SerializedName("color") val color: Int,
)

data class TeacherDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("subjectName") val subjectName: String? = null,
)

data class SubjectProgressDto(
    @SerializedName("completed") val completed: Int = 0,
    @SerializedName("total") val total: Int = 0,
)

data class TimetableSnapshotDto(
    @SerializedName("schemaVersion") val schemaVersion: Int = 1,
    @SerializedName("timetables") val timetables: List<TimetableDto> = emptyList(),
)

data class TimetableChangeDto(
    @SerializedName("kind") val kind: String,
    @SerializedName("timetableIds") val timetableIds: List<String> = emptyList(),
    @SerializedName("eventIds") val eventIds: List<String> = emptyList(),
    @SerializedName("subjectIds") val subjectIds: List<String> = emptyList(),
)

data class IcsExportResultDto(
    @SerializedName("filePath") val filePath: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("eventCount") val eventCount: Int = 0,
)

data class IcsImportRequestDto(
    @SerializedName("fileName") val fileName: String? = null,
    @SerializedName("bytes") val bytes: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IcsImportRequestDto) return false
        return fileName == other.fileName && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * (fileName?.hashCode() ?: 0) + bytes.contentHashCode()
}

data class IcsImportResultDto(
    @SerializedName("timetableId") val timetableId: String,
    @SerializedName("timetableName") val timetableName: String,
    @SerializedName("importedCount") val importedCount: Int,
)

// 第二波：课程池。先落 DTO，服务迁移不在本阶段。
data class CatalogCourseDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String = "",
    @SerializedName("code") val code: String? = null,
    @SerializedName("key") val key: String? = null,
    @SerializedName("credit") val credit: Float = 0f,
    @SerializedName("field") val field: String? = null,
    @SerializedName("selectCategory") val selectCategory: String? = null,
    @SerializedName("nature") val nature: String? = null,
    @SerializedName("school") val school: String? = null,
    @SerializedName("teacher") val teacher: String? = null,
    @SerializedName("externalCourseId") val externalCourseId: String? = null,
    @SerializedName("taskNumber") val taskNumber: String? = null,
    @SerializedName("selectionRequestId") val selectionRequestId: String? = null,
)
