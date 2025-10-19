package com.micrantha.eyespie.features.login.entities

import com.micrantha.eyespie.domain.entities.Session

sealed interface LoginAction {

    data object OnLogin : LoginAction

    data object OnLoginWithGoogle : LoginAction

    data class OnError(val err: Throwable) : LoginAction

    data class OnSuccess(val session: Session) : LoginAction

    data object ToggleEmailMask : LoginAction

    data object TogglePasswordMask : LoginAction

    data class ChangedPassword(val password: String) : LoginAction

    data class ChangedEmail(val email: String) : LoginAction

    data object ResetStatus : LoginAction

    data object OnRegister : LoginAction
}
