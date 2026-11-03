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
     */
    fun loadAndInstall(dexFile: File, entryClassName: String, optimizedDir: File): Plugin {
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
            throw IllegalStateException("插件与宿主 API 不兼容（可能是 api 版本不匹配）：$entryClassName", e)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("插件中找不到入口类：$entryClassName", e)
        }

        val plugin = try {
            clazz.getDeclaredConstructor().newInstance() as Plugin
        } catch (e: ClassCastException) {
            throw IllegalStateException("入口类未实现 Plugin 接口：$entryClassName", e)
        }

        host.install(plugin)
        host.failures()[plugin.id]?.let { failure ->
            throw IllegalStateException("插件 ${plugin.id} 装载失败", failure)
        }
        logger("runtime plugin installed: ${plugin.id}", null)
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
