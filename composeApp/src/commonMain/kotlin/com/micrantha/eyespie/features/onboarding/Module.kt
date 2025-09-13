package com.micrantha.eyespie.features.onboarding

import com.micrantha.bluebell.get
import com.micrantha.eyespie.features.onboarding.data.OnboardingLocalSource
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.ui.OnboardingEnvironment
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreenModel
import com.micrantha.eyespie.features.onboarding.usecase.DownloadModelsUseCase
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingletonOf

internal fun onboardingModule() = DI.Module("Onboarding") {
    bindSingletonOf(::OnboardingLocalSource)
    bindProviderOf(::OnboardingRepository)

    bindProviderOf(::OnboardingEnvironment)
    bindProvider { DownloadModelsUseCase(get(), get(), get(arg = "models")) }
    bindProviderOf(::OnboardingScreen)
    bindProvider { OnboardingScreenModel(get(), get()) }
}
