package com.stupidtree.hitax.eas

import android.app.Application
import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.dto.EasAccountDto
import com.stupidtree.hitax.data.model.eas.EASToken
import com.stupidtree.hitax.data.repository.EASRepository

/**
 * 账号快照实现。只读取 token 并映射 DTO；不暴露 token / cookie / password。
 */
internal class EasAccountServiceImpl(
    private val tokenProvider: () -> EASToken,
) : EasAccountService {

    constructor(application: Application) : this({
        EASRepository.getInstance(application).getEasToken()
    })

    override fun account(): EasAccountDto = tokenProvider().toAccountDto()
}
