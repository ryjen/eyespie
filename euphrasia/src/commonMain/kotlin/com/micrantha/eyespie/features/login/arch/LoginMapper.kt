package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.eyespie.features.login.entities.LoginState
import com.micrantha.eyespie.features.login.entities.LoginUiState

class LoginMapper: StateMapper<LoginState, LoginUiState> {

    companion object : StateMapper<LoginState, LoginUiState> {
        private var uiState: LoginUiState? = null

        override fun map(state: LoginState) = uiState?.copy(
            email = state.email,
            password = state.password,
            status = state.status,
            isEmailMasked = state.isEmailMasked ?: state.email.isNotBlank(),
            isPasswordMasked = state.isPasswordMasked
        ) ?: LoginUiState(
            email = state.email,
            password = state.password,
            status = state.status,
            isEmailMasked = state.isEmailMasked ?: state.email.isNotBlank(),
            isPasswordMasked = state.isPasswordMasked
        ).also {
            uiState = it
        }
    }

    override fun map(state: LoginState) = LoginMapper.map(state)
}
