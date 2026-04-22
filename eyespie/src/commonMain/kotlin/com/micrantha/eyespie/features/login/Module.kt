package com.micrantha.eyespie.features.login

import com.micrantha.bluebell.get
import com.micrantha.eyespie.features.login.arch.LoginEffects
import com.micrantha.eyespie.features.login.ui.LoginScreen
import com.micrantha.eyespie.features.login.ui.LoginScreenModel
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf

internal fun module() = DI.Module("Login") {
    bindProviderOf(::LoginEffects)
    bindProvider { LoginScreenModel(get(), get()) }
    bindProviderOf(::LoginScreen)
}
