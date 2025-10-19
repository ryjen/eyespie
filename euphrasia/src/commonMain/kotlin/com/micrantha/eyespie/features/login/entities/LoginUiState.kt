package com.micrantha.eyespie.features.login.entities

import com.micrantha.bluebell.ui.model.UiResult

data class LoginUiState(
    val email: String,
    val password: String,
    val status: UiResult<Unit>,
    val isEmailMasked: Boolean,
    val isPasswordMasked: Boolean
)
