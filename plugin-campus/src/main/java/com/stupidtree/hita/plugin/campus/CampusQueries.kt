package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusServiceTarget
import com.stupidtree.hita.plugin.campus.source.CampusCardNetworkException
import com.stupidtree.hita.plugin.campus.source.CampusCardParseException
import com.stupidtree.hita.plugin.campus.source.CampusCardRemoteException
import com.stupidtree.hita.plugin.campus.source.CampusCardSignInRequiredException
import com.stupidtree.hita.plugin.campus.source.LabCenterException
import com.stupidtree.hita.plugin.campus.source.LabCenterSessionExpiredException
import com.stupidtree.hita.plugin.campus.source.LibrarySmartWallException
import com.stupidtree.hita.plugin.campus.source.LibrarySmartWallSignInRequiredException
import com.stupidtree.hita.plugin.campus.source.SilentPodException
import com.stupidtree.hita.plugin.campus.source.SilentPodFailureKind
import kotlinx.coroutines.CancellationException

/**
 * 执行一次门户查询，并把内部异常翻译成 [CampusResult]。
 *
 * 协程取消必须原样抛出；其余异常按门户分类，调用方不需要 catch。
 */
internal suspend fun <T> campusQuery(
    target: CampusServiceTarget,
    block: suspend () -> T,
): CampusResult<T> = try {
    CampusResult.Success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    error.toCampusResult(target)
}

private fun Exception.toCampusResult(target: CampusServiceTarget): CampusResult<Nothing> = when (this) {
    is CampusCardSignInRequiredException -> CampusResult.NotSignedIn(target)
    is CampusCardNetworkException -> CampusResult.NetworkError(messageOf("校园门户网络不可用"))
    is CampusCardRemoteException -> CampusResult.RemoteError(messageOf("校园门户返回错误"))
    is CampusCardParseException -> CampusResult.ParseError(messageOf("校园门户响应无法识别"))
    is SilentPodException -> when (kind) {
        SilentPodFailureKind.AUTHENTICATION -> CampusResult.NotSignedIn(target)
        SilentPodFailureKind.GATEWAY,
        SilentPodFailureKind.NETWORK,
        SilentPodFailureKind.TIMEOUT -> CampusResult.NetworkError(messageOf("图书馆服务暂时不可用"))
        SilentPodFailureKind.API -> CampusResult.RemoteError(messageOf("预约服务返回错误"), code)
        SilentPodFailureKind.PARSE -> CampusResult.ParseError(messageOf("预约服务响应无法识别"))
    }
    is LibrarySmartWallSignInRequiredException -> CampusResult.NotSignedIn(target)
    is LibrarySmartWallException -> CampusResult.RemoteError(messageOf("图书馆服务暂时不可用"))
    is LabCenterSessionExpiredException -> CampusResult.NotSignedIn(target)
    is LabCenterException -> CampusResult.RemoteError(messageOf("实验中心服务暂时不可用"))
    else -> CampusResult.RemoteError(messageOf("校园服务查询失败"))
}

private fun Exception.messageOf(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
