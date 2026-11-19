package com.stupidtree.hitax.market

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 预设的 tabs 字段解析：老索引没有该字段时必须退化为 null（宿主走内置布局）。 */
class PresetTabsParseTest {

    private val gson = Gson()

    @Test
    fun parsesTabsInUpstreamOrder() {
        val json = """
            {"id":"hita-android-3.1.2","name":"HITA Android 3.1.2 复刻","tabs":[
              {"id":"com.stupidtree.hita.ui.homework","title":"待办","order":0},
              {"id":"hita.timeline","title":"今日","order":1},
              {"id":"hita.timetable","title":"时间表","order":2},
              {"id":"com.stupidtree.hita.ui.ai","title":"助手","order":3},
              {"id":"theta.feed","title":"资讯","order":4},
              {"id":"hita.more","title":"更多","order":5}
            ]}
        """.trimIndent()

        val preset = gson.fromJson(json, MarketPreset::class.java)
        val tabs = preset.tabs.orEmpty().sortedBy { it.order }

        assertEquals(6, tabs.size)
        assertEquals(
            listOf("待办", "今日", "时间表", "助手", "资讯", "更多"),
            tabs.map { it.title },
        )
        assertEquals("hita.more", tabs.last().id)
    }

    @Test
    fun missingTabsKeepsLegacyBehaviour() {
        val preset = gson.fromJson("""{"id":"hita-l-1.5.3","name":"HITA-L"}""", MarketPreset::class.java)
        assertNull(preset.tabs)
    }
}