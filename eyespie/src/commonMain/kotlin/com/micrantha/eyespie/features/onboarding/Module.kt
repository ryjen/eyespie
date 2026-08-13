package com.micrantha.eyespie.features.onboarding

import com.micrantha.bluebell.get
import com.micrantha.eyespie.domain.usecase.InitGenAIUseCase
import com.micrantha.eyespie.features.onboarding.arch.OnboardingEffects
import com.micrantha.eyespie.features.onboarding.data.CapabilityPermissionGateway
import com.micrantha.eyespie.features.onboarding.data.DataOnboardingRepository
import com.micrantha.eyespie.features.onboarding.data.ModelMetaRepository
import com.micrantha.eyespie.features.onboarding.data.MokoCapabilityPermissionGateway
import com.micrantha.eyespie.features.onboarding.data.OnboardingLocalSource
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.ui.OnboardingScreenModel
import com.micrantha.eyespie.features.onboarding.ui.genai.GenAIDownloadScreen
import com.micrantha.eyespie.features.onboarding.ui.genai.GenAiDownloadScreenModel
import com.micrantha.eyespie.features.onboarding.usecase.DefaultLoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityVerifier
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindProviderOf
import org.kodein.di.bindSingletonOf
import org.kodein.di.delegate

internal fun onboardingModule() = DI.Module("Onboarding") {
    bindSingletonOf(::OnboardingLocalSource)
    bindProviderOf(::DataOnboardingRepository)
    delegate<OnboardingRepository>().to<DataOnboardingRepository>()

    bindProvider { ModelIntegrityVerifier() }
    bindProviderOf(::InitGenAIUseCase)
    bindProviderOf(::DefaultLoadModelConfig)
    delegate<LoadModelConfig>().to<DefaultLoadModelConfig>()

    bindProviderOf(::ModelMetaRepository)

    bindProviderOf(::MokoCapabilityPermissionGateway)
    delegate<CapabilityPermissionGateway>().to<MokoCapabilityPermissionGateway>()
    bindProviderOf(::OnboardingEffects)

    bindProviderOf(::OnboardingScreen)
    bindProvider { OnboardingScreenModel(get(), get()) }

    bindProviderOf(::GenAIDownloadScreen)
    bindProvider {
        GenAiDownloadScreenModel(get(), get(), get("onboarding"), get(), get(), get(), get())
    }
}
