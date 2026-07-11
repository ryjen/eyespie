package com.micrantha.eyespie.features.login.entities

import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.app.AppConfig
import org.jetbrains.compose.resources.StringResource

data class LoginState(
    val email: String = AppConfig.LOGIN_EMAIL,
    val password: String = AppConfig.LOGIN_PASSWORD,
    val status: UiResult<Unit> = UiResult.Default,
    val isPasswordMasked: Boolean = true,
    val isEmailMasked: Boolean? = null,
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null
)
