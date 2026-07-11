package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.ui.model.UiResult.Busy
import com.micrantha.bluebell.ui.model.UiResult.Default
import com.micrantha.bluebell.ui.model.UiResult.Failure
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.features.login.entities.LoginAction.ChangedEmail
import com.micrantha.eyespie.features.login.entities.LoginAction.ChangedPassword
import com.micrantha.eyespie.features.login.entities.LoginAction.NotConfigured
import com.micrantha.eyespie.features.login.entities.LoginAction.OnError
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLogin
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLoginWithGoogle
import com.micrantha.eyespie.features.login.entities.LoginAction.OnSuccess
import com.micrantha.eyespie.features.login.entities.LoginAction.ResetStatus
import com.micrantha.eyespie.features.login.entities.LoginAction.ToggleEmailMask
import com.micrantha.eyespie.features.login.entities.LoginAction.TogglePasswordMask
import com.micrantha.eyespie.features.login.entities.LoginState
import com.micrantha.eyespie.generated.resources.email_required
import com.micrantha.eyespie.generated.resources.logging_in
import com.micrantha.eyespie.generated.resources.login_failed
import com.micrantha.eyespie.generated.resources.not_configured
import com.micrantha.eyespie.generated.resources.password_required

class LoginReducer : Reducer<LoginState> {

    override fun reduce(state: LoginState, action: Action) = when (action) {
        is ChangedEmail -> state.copy(
            email = action.email,
            emailError = null
        )
        is ChangedPassword -> state.copy(
            password = action.password,
            passwordError = null
        )
        is OnSuccess -> state.copy(status = Default)
        is ToggleEmailMask -> state.copy(
            isEmailMasked = state.isEmailMasked?.not() ?: state.email.isBlank()
        )

        is TogglePasswordMask -> state.copy(isPasswordMasked = !state.isPasswordMasked)
        is OnLogin -> {
            val emailError = if (state.email.isBlank()) S.email_required else null
            val passwordError = if (state.password.isBlank()) S.password_required else null

            if (emailError != null || passwordError != null) {
                state.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            } else {
                state.copy(status = Busy(S.logging_in))
            }
        }
        is OnLoginWithGoogle -> state.copy(status = Busy(S.logging_in))
        is OnError -> state.copy(status = Failure(S.login_failed))
        is NotConfigured -> {
            println("LoginReducer: NotConfigured → Failure")
            state.copy(status = Failure(S.not_configured))
        }
        is ResetStatus -> state.copy(status = Default)
        else -> state
    }

}
