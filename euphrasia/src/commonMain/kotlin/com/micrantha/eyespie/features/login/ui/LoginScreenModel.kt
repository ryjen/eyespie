package com.micrantha.eyespie.features.login.ui

import com.micrantha.bluebell.ui.screen.MappedScreenModel
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.login.arch.LoginEffects
import com.micrantha.eyespie.features.login.arch.LoginMapper
import com.micrantha.eyespie.features.login.arch.LoginReducer
import com.micrantha.eyespie.features.login.entities.LoginState
import com.micrantha.eyespie.features.login.entities.LoginUiState

class LoginScreenModel(
    screenContext: ScreenContext,
    effects: LoginEffects,
    reducer: LoginReducer = LoginReducer(),
    mapper: LoginMapper = LoginMapper(),
    initialState: LoginState = LoginState()
) : MappedScreenModel<LoginState, LoginUiState>(
    screenContext,
    initialState,
    mapper
) {

    init {
        store.addReducer(reducer::reduce)
            .applyEffect(effects::invoke)
    }
}
