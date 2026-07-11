package com.micrantha.eyespie.app

import com.micrantha.eyespie.app.ui.MainScreen
import com.micrantha.eyespie.app.ui.MainScreenModel
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import com.micrantha.eyespie.app.usecase.LoadMainUseCaseImpl
import com.micrantha.eyespie.core.data.client.DefaultSupabaseConfigChecker
import com.micrantha.eyespie.core.data.client.SupabaseConfigChecker
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.delegate
import com.micrantha.eyespie.core.module as coreModule
import com.micrantha.eyespie.domain.module as domainModule
import com.micrantha.eyespie.features.module as featuresModule

internal fun module() = DI.Module("App") {
    importOnce(coreModule())
    importOnce(domainModule())
    importOnce(featuresModule())

    bindProviderOf(::MainScreenModel)
    bindProviderOf(::MainScreen)
    bindProviderOf(::LoadMainUseCaseImpl)
    delegate<LoadMainUseCase>().to<LoadMainUseCaseImpl>()
    bindProvider<SupabaseConfigChecker> { DefaultSupabaseConfigChecker() }
}
