package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.ui.model.UiResult.Busy
import com.micrantha.bluebell.ui.model.UiResult.Default
import com.micrantha.bluebell.ui.model.UiResult.Failure
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.features.login.entities.LoginAction.*
import com.micrantha.eyespie.features.login.entities.LoginState
import eyespie.euphrasia.generated.resources.logging_in
import eyespie.euphrasia.generated.resources.login_failed

class LoginReducer : Reducer<LoginState> {

    override fun reduce(state: LoginState, action: Action) = when (action) {
        is ChangedEmail -> state.copy(email = action.email)
        is ChangedPassword -> state.copy(password = action.password)
        is OnSuccess -> state.copy(status = Default)
        is ToggleEmailMask -> state.copy(
            isEmailMasked = state.isEmailMasked?.not() ?: state.email.isBlank()
        )

        is TogglePasswordMask -> state.copy(isPasswordMasked = !state.isPasswordMasked)
        is OnLogin, is OnLoginWithGoogle -> state.copy(status = Busy(S.logging_in))
        is OnError -> state.copy(status = Failure(S.login_failed))
        is ResetStatus -> state.copy(status = Default)
        else -> state
    }

}
