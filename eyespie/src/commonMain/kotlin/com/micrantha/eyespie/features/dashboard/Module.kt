package com.micrantha.eyespie.features.dashboard

import com.micrantha.bluebell.get
import com.micrantha.eyespie.features.dashboard.ui.DashboardEnvironment
import com.micrantha.eyespie.features.dashboard.ui.DashboardScreen
import com.micrantha.eyespie.features.dashboard.ui.DashboardScreenModel
import com.micrantha.eyespie.features.dashboard.ui.usecase.DashboardLoadUseCase
import com.micrantha.eyespie.features.dashboard.ui.usecase.DashboardLoadUseCaseImpl
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf


internal fun module() = DI.Module("Dashboard") {

    bindProviderOf(::DashboardScreen)
    bindProvider<DashboardLoadUseCase> { DashboardLoadUseCaseImpl(get(), get(), get()) }
    bindProviderOf(::DashboardEnvironment)
    bindProvider { DashboardScreenModel(get()) }
}
