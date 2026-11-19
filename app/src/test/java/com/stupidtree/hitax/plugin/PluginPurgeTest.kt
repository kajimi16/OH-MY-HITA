package com.stupidtree.hitax.plugin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginPurgeTest {

    /**
     * 用户在 1.2 秒的延迟窗口内重新启用插件时，cancel 会清掉待清理标记，
     * 此时已经排队的清理任务必须自行跳过，否则会删掉刚重建的数据。
     */
    @Test
    fun canceledPurgeIsSkipped() {
        assertFalse(shouldPurge(emptySet(), "com.example.plugin"))
    }

    @Test
    fun stillPendingPurgeRuns() {
        assertTrue(shouldPurge(setOf("com.example.plugin"), "com.example.plugin"))
    }

    /** 清理集合里有别的插件时，不能顺手删掉当前这个。 */
    @Test
    fun otherPendingPluginDoesNotEnablePurge() {
        assertFalse(shouldPurge(setOf("com.example.other"), "com.example.plugin"))
    }
}