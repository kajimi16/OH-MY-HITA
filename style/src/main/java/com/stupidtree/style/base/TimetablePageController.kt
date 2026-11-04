package com.stupidtree.style.base

/**
 * 课表页与宿主主界面之间的标题联动契约。
 *
 * 放在 :style 里，宿主与插件都可以实现/使用，避免宿主 import 插件里的嵌套接口。
 */
interface TimetablePageController {
    fun setTitleText(string: String)
    fun setTimetableName(String: String)
    fun setSingleTitle(string: String)
}
