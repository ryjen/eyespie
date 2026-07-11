package com.micrantha.eyespie.features.login.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import com.micrantha.eyespie.core.data.client.SupabaseConfigChecker
import com.micrantha.eyespie.domain.repository.AccountRepository
import com.micrantha.eyespie.features.login.entities.LoginAction.OnError
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLogin
import com.micrantha.eyespie.features.login.entities.LoginAction.OnLoginWithGoogle
import com.micrantha.eyespie.features.login.entities.LoginAction.OnRegister
import com.micrantha.eyespie.features.login.entities.LoginAction.OnSuccess
import com.micrantha.eyespie.features.login.entities.LoginState
import com.micrantha.eyespie.features.login.entities.LoginAction.NotConfigured
import com.micrantha.eyespie.features.register.ui.RegisterScreen

class LoginEffects(
    private val context: ScreenContext,
    private val accountRepository: AccountRepository,
    private val loadMainUseCase: LoadMainUseCase,
    private val supabaseConfigChecker: SupabaseConfigChecker,
) : Effect<LoginState>,
    Dispatcher by context.dispatcher, Router by context.router {

    override suspend fun invoke(action: Action, state: LoginState) {
        when (action) {
            is OnLogin -> {
                if (state.emailError != null || state.passwordError != null) {
                    return
                }
                if (!supabaseConfigChecker.isSupabaseConfigured()) {
                    dispatch(NotConfigured)
                    return
                }
                accountRepository.login(state.email, state.password)
                    .onFailure {
                        dispatch(OnError(it))
                    }.onSuccess {
                        dispatch(OnSuccess(it))
                    }
            }

            is OnLoginWithGoogle -> {
                if (!supabaseConfigChecker.isSupabaseConfigured()) {
                    dispatch(NotConfigured)
                    return
                }
                accountRepository.loginWithGoogle()
                    .onSuccess {
                        dispatch(OnSuccess(it))
                    }.onFailure {
                        dispatch(OnError(it))
                    }
            }

            is OnRegister -> context.navigate<RegisterScreen>()

            is OnSuccess -> loadMainUseCase().onFailure {
                dispatch(OnError(it))
            }
        }
    }
}
