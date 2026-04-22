package com.micrantha.eyespie.features.onboarding

import com.micrantha.bluebell.get
import com.micrantha.eyespie.domain.usecase.InitGenAIUseCase
import com.micrantha.eyespie.features.onboarding.arch.OnboardingEffects
import com.micrantha.eyespie.features.onboarding.data.ModelMetaRepository
import com.micrantha.eyespie.features.onboarding.data.OnboardingLocalSource
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreenModel
import com.micrantha.eyespie.features.onboarding.ui.genai.GenAIDownloadScreen
import com.micrantha.eyespie.features.onboarding.ui.genai.GenAiDownloadScreenModel
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingletonOf

internal fun onboardingModule() = DI.Module("Onboarding") {
    bindSingletonOf(::OnboardingLocalSource)
    bindProviderOf(::OnboardingRepository)

    bindProviderOf(::InitGenAIUseCase)
    bindProviderOf(::LoadModelConfig)

    bindProviderOf(::ModelMetaRepository)

    bindProviderOf(::OnboardingEffects)

    bindProviderOf(::OnboardingScreen)
    bindProvider { OnboardingScreenModel(get(), get()) }

    bindProviderOf(::GenAIDownloadScreen)
    bindProvider { GenAiDownloadScreenModel(get(), get(), get("onboarding"), get(), get(), get()) }
}
