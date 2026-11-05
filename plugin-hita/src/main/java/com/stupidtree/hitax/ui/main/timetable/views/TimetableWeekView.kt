package com.stupidtree.hitax.ui.main.timetable.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.stupidtree.hita.plugin.timetable.R
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.ui.main.timetable.TimetableStyleSheet
import java.util.*

class TimetableWeekView: LinearLayout{

    private var timetableView: TimeTableView? = null
    private val topDateTexts = arrayOfNulls<TextView>(8) //顶部日期文本

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context?) : super(context)

    fun init() {
        timetableView?.init()
    }

    fun getStyleSheet(): TimetableStyleSheet? {
        return timetableView?.styleSheet
    }

    fun getEventsViewNum(): Int {
        return timetableView?.childCount ?: 0
    }


    fun setOnCardClickListener(obj: TimeTableView.OnCardClickListener) {
        timetableView?.setOnCardClickListener(obj)
    }

    fun setOnAddClickListener(obj: TimeTableView.OnAddClickListener) {
        timetableView?.setOnAddClickListener(obj)
    }

    fun setOnCardLongClickListener(obj: TimeTableView.OnCardLongClickListener) {
        timetableView?.setOnCardLongClickListener(obj)
    }


    fun refresh(
        startDate: Long,
        events: List<EventItem>,
        style: TimetableStyleSheet,
        dateOnly: Boolean = false
    ) {
        setDateTexts(startDate)
        if (dateOnly) timetableView?.setStartDate(startDate)
        else timetableView?.notifyRefresh(startDate, events, style)
    }

    private val dateColors = mutableMapOf<Int, Int>()
    private var wallpaperTextEnabled = false
    fun setWallpaperText(enabled: Boolean) {
        if (enabled == wallpaperTextEnabled) return
        wallpaperTextEnabled = enabled
        if (enabled) {
            topDateTexts.forEachIndexed { index, text -> text?.let {
                dateColors[index] = it.currentTextColor
                it.setTextColor(android.graphics.Color.WHITE)
                it.setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
            } }
        } else {
            topDateTexts.forEachIndexed { index, text -> text?.let {
                dateColors[index]?.let { color -> it.setTextColor(color) }
                it.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
            } }
            dateColors.clear()
        }
    }
    fun setDateTexts(date: Long) {
        setWallpaperText(com.stupidtree.hitax.ui.main.timetable.TimetableAppearanceStore.get(context).observe().value?.hasWallpaper == true)
        val startDate = Calendar.getInstance()
        startDate.timeInMillis = date
        /*显示上方日期*/
        topDateTexts[0]?.text =
            context.resources.getStringArray(R.array.months)[startDate[Calendar.MONTH]]
        val temp = Calendar.getInstance()
        for (k in 1..7) {
            temp.time = startDate.time
            temp.add(Calendar.DATE, k - 1)
            topDateTexts[k]!!.text = temp[Calendar.DAY_OF_MONTH].toString()
        }
    }

    init {
        inflate(context, R.layout.view_timetable_week, this)
        orientation = VERTICAL
        val lp = LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT)
        layoutParams = lp
        timetableView = findViewById(R.id.timetableView)
        topDateTexts[0] = findViewById(R.id.tt_tv_month)
        topDateTexts[1] = findViewById(R.id.tt_tv_day1)
        topDateTexts[2] = findViewById(R.id.tt_tv_day2)
        topDateTexts[3] = findViewById(R.id.tt_tv_day3)
        topDateTexts[4] = findViewById(R.id.tt_tv_day4)
        topDateTexts[5] = findViewById(R.id.tt_tv_day5)
        topDateTexts[6] = findViewById(R.id.tt_tv_day6)
        topDateTexts[7] = findViewById(R.id.tt_tv_day7)
    }
}