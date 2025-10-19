package com.micrantha.eyespie.features.login.entities

import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.app.AppConfig

data class LoginState(
    val email: String = AppConfig.LOGIN_EMAIL,
    val password: String = AppConfig.LOGIN_PASSWORD,
    val status: UiResult<Unit> = UiResult.Default,
    val isPasswordMasked: Boolean = true,
    val isEmailMasked: Boolean? = null
)
