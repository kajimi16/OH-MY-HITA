package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.Plugin

/**
 * 按配置挑选要装配的插件。
 *
 * 这是 profile.yml 的最小实现：`assets/plugins.json` 里列出启用的插件 id，
 * 缺省（文件不存在或列表为空）表示全部启用。
 */
object PluginComposition {

    fun select(all: List<Plugin>, enabled: Set<String>?): List<Plugin> {
        if (enabled == null || enabled.isEmpty()) return all
        return all.filter { it.id in enabled }
    }
}
