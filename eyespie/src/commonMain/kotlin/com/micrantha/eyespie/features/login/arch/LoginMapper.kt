package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.eyespie.features.login.entities.LoginState
import com.micrantha.eyespie.features.login.entities.LoginUiState

class LoginMapper : StateMapper<LoginState, LoginUiState> {

    override fun map(state: LoginState) = LoginUiState(
        email = state.email,
        password = state.password,
        status = state.status,
        isEmailMasked = state.isEmailMasked ?: state.email.isNotBlank(),
        isPasswordMasked = state.isPasswordMasked,
        emailError = state.emailError,
        passwordError = state.passwordError
    )
}
