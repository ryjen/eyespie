package com.micrantha.eyespie.features.onboarding

import com.micrantha.bluebell.com.micrantha.eyespie.features.onboarding.ui.OnboardingEffects
import com.micrantha.bluebell.get
import com.micrantha.eyespie.features.onboarding.data.OnboardingLocalSource
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.domain.usecase.DownloadModelUseCase
import com.micrantha.eyespie.features.onboarding.domain.usecase.InitGenAIUseCase
import com.micrantha.eyespie.features.onboarding.domain.usecase.LoadModelConfigUseCase
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreenModel
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingletonOf

internal fun onboardingModule() = DI.Module("Onboarding") {
    bindSingletonOf(::OnboardingLocalSource)
    bindProviderOf(::OnboardingRepository)

    bindProvider { DownloadModelUseCase(get(), get(arg = "onboarding")) }
    bindProviderOf(::InitGenAIUseCase)
    bindProviderOf(::LoadModelConfigUseCase)

    bindProviderOf(::OnboardingEffects)

    bindProviderOf(::OnboardingScreen)
    bindProvider { OnboardingScreenModel(get(), get()) }
}
