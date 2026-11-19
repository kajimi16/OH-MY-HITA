package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.Plugin
import dalvik.system.DexClassLoader
import java.io.File
import java.security.MessageDigest

/**
 * 运行时插件加载器（T1）。
 *
 * 加载的是**纯逻辑插件**：dex 里只有插件自己的类，`:api` / `:model` 由宿主提供。
 * 插件不能声明四大组件，也不能带资源——这是 Android 的硬边界，不是实现偷懒。
 */
class PluginLoader(
    private val host: PluginHost,
    private val hostClassLoader: ClassLoader = PluginLoader::class.java.classLoader!!,
    private val logger: (message: String, error: Throwable?) -> Unit = { _, _ -> },
) {

    /**
     * 从 dex 文件装载并安装插件。
     *
     * @param dexFile       已下载并校验过的 dex
     * @param entryClassName 实现 [Plugin] 的类全名
     * @param optimizedDir  该插件专属的 odex 目录
     * @param meta          加载前元数据；提供时先做兼容性拒绝，再碰 dex/类加载
     * @param descriptor    依赖声明；为空时按无声明插件处理
     */
    fun loadAndInstall(
        dexFile: File,
        entryClassName: String,
        optimizedDir: File,
        meta: PluginArtifactMeta? = null,
        descriptor: PluginDescriptor? = null,
        hostAbi: String? = null,
        exempt: Boolean = false,
        sdkInt: Int? = null,
    ): Plugin {
        if (meta != null) {
            when (val result = PluginCompatibility.check(
                meta,
                hostAbi = hostAbi,
                sdkInt = sdkInt,
                exempt = exempt,
            )) {
                is CompatibilityResult.Rejected -> {
                    val error = PluginRejectedException(meta.id, result.reasons)
                    host.reject(meta.id, error, PluginFailurePhase.COMPATIBILITY)
                    throw error
                }
                CompatibilityResult.Compatible -> Unit
            }
        }
        require(dexFile.isFile) { "plugin dex missing: ${dexFile.absolutePath}" }
        optimizedDir.mkdirs()

        val loader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            hostClassLoader,
        )

        val clazz = try {
            Class.forName(entryClassName, true, loader)
        } catch (e: LinkageError) {
            val error = IllegalStateException("插件与宿主 API 不兼容（可能是 api 版本不匹配）：$entryClassName", e)
            host.reject(meta?.id ?: entryClassName, error, PluginFailurePhase.LOAD)
            throw error
        } catch (e: ClassNotFoundException) {
            val error = IllegalStateException("插件中找不到入口类：$entryClassName", e)
            host.reject(meta?.id ?: entryClassName, error, PluginFailurePhase.LOAD)
            throw error
        }

        val plugin = try {
            clazz.getDeclaredConstructor().newInstance() as Plugin
        } catch (e: ClassCastException) {
            val error = IllegalStateException("入口类未实现 Plugin 接口：$entryClassName", e)
            host.reject(meta?.id ?: entryClassName, error, PluginFailurePhase.LOAD)
            throw error
        }

        host.install(plugin, descriptor ?: PluginDescriptor(plugin.id))
        host.failures()[plugin.id]?.let { failure ->
            throw IllegalStateException("插件 ${plugin.id} 装载失败", failure)
        }
        if (host.isInstalled(plugin.id)) {
            logger("runtime plugin installed: ${plugin.id}", null)
        } else {
            logger("runtime plugin waiting for services: ${plugin.id}", null)
        }
        return plugin
    }

    companion object {
        /** 计算文件 sha256，用于校验下载到的插件。 */
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
