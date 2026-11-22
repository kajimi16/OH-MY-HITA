package com.stupidtree.hitax.legacy.lite.ui.eas.imp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import java.util.Calendar

/**
 * HITA_Lite 导入课表门面。
 *
 * 前置条件：宿主 TimetableService 已注入；缺失时页面只读且拒绝写入。
 * 后置条件：页面展示当前课表的开始时间与课时结构，修改提交给宿主课表服务；
 * 教务导入本身由宿主 EasService 完成，本模块不接触网络、会话与 EAS 仓库。
 */
class LiteImportTimetableViewModel : ViewModel() {

    private val timetableService = LiteHostBridge.timetable
    private val easService = LiteHostBridge.eas

    val timetableLiveData: LiveData<Timetable?> =
        timetableService?.getRecentTimetable() ?: MutableLiveData<Timetable?>(null)

    /** 宿主是否提供教务导入入口。 */
    fun hasEas(): Boolean = easService != null

    /** 当前课表结构快照。 */
    fun currentStructure(): List<TimePeriodInDay> =
        timetableLiveData.value?.scheduleStructure.orEmpty()

    /** 当前课表开始时间；没有课表时返回今天。 */
    fun startDate(): Calendar {
        val calendar = Calendar.getInstance()
        timetableLiveData.value?.let { calendar.timeInMillis = it.startTime.time }
        return calendar
    }

    fun changeStartDate(calendar: Calendar) {
        val timetable = timetableLiveData.value ?: return
        timetableService?.actionChangeTimetableStartDate(timetable, calendar.timeInMillis)
    }

    fun setStructureData(period: TimePeriodInDay, position: Int) {
        val timetable = timetableLiveData.value ?: return
        if (position !in timetable.scheduleStructure.indices) return
        timetableService?.actionChangeTimetableStructure(timetable, period, position)
    }
}
