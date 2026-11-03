package com.stupidtree.hita.api

/**
 * 服务的类型化句柄。**契约写在接口里，不写在数据类里**：
 * 跨插件只传 DTO 与 id，不共享 @Entity。
 */
class ServiceKey<T : Any>(val name: String) {
    override fun toString(): String = "ServiceKey($name)"
}
