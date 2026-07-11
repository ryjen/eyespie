package com.micrantha.eyespie.features.login.entities

import com.micrantha.bluebell.ui.model.UiResult
import org.jetbrains.compose.resources.StringResource

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val status: UiResult<Unit> = UiResult.Default,
    val isEmailMasked: Boolean = false,
    val isPasswordMasked: Boolean = true,
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null
)
