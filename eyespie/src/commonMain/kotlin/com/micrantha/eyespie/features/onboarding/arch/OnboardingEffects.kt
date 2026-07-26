package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.ext.getIf
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.CapabilityPermissionGateway
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilitiesLoaded
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestFailed
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestResolved
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestStarted
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Done
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Download
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Error
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Init
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Loaded
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.NextPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.OpenCapabilitySettings
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.PageChanged
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.RefreshCapabilities
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.RequestCapability
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.SkipDownload
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

class OnboardingEffects(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val loadMainUseCase: LoadMainUseCase,
    private val loadModelConfig: LoadModelConfig,
    private val capabilityPermissionGateway: CapabilityPermissionGateway,
) : Effect<OnboardingState>, Dispatcher by context.dispatcher {
    private val requestCoordinator = CapabilityRequestCoordinator()

    override suspend fun invoke(
        action: Action,
        state: OnboardingState,
    ) {
        when (action) {
            is Init -> {
                refreshCapabilities(state)
                loadModelConfig().onSuccess {
                    dispatch(Loaded(it))
                }.onFailure {
                    dispatch(Error(it))
                }
            }

            is RefreshCapabilities -> refreshCapabilities(state)

            is PageChanged -> {
                if (OnboardingPage.entries.getOrNull(action.page) == OnboardingPage.Permissions) {
                    refreshCapabilities(state)
                }
            }

            is RequestCapability -> requestCapability(action, state)

            is OpenCapabilitySettings -> {
                val authorization = state.capabilities
                    .firstOrNull { it.capability == action.capability }
                    ?.authorization
                if (authorization == CapabilityAuthorization.SettingsRequired) {
                    capabilityPermissionGateway.openSettings(action.capability)
                }
            }

            is SkipDownload -> {
                dispatch(NextPage)
            }

            is Download -> getIf(state.models, state.selectedModel)?.let { (model, _) ->
                onboardingRepository.setGenAiModel(model)
                dispatch(Done)
            }

            is Done -> {
                onboardingRepository.setHasRunOnce()
                loadMainUseCase().onFailure {
                    dispatch(Error(it))
                }
            }

            is NextPage -> {
                val next = OnboardingPage.entries.getOrNull(state.page.ordinal + 1)
                if (next == null) {
                    dispatch(Done)
                } else {
                    dispatch(PageChanged(next.ordinal))
                }
            }

            else -> Unit
        }
    }

    private suspend fun refreshCapabilities(state: OnboardingState) {
        if (state.requestInFlight != null) return

        val previous = state.capabilities
        dispatch(
            CapabilitiesLoaded(
                previous = previous,
                capabilities = capabilityPermissionGateway.loadCapabilities(previous),
            )
        )
    }

    private suspend fun requestCapability(
        action: RequestCapability,
        state: OnboardingState,
    ) {
        val capability = state.capabilities.firstOrNull {
            it.capability == action.capability && it.canRequestDuringOnboarding
        } ?: return
        if (state.requestInFlight != null) return

        try {
            val authorization = requestCoordinator.runExclusive {
                dispatch(CapabilityRequestStarted(action.capability))
                capabilityPermissionGateway.requestAuthorization(
                    capability = action.capability,
                    previous = capability.authorization,
                )
            } ?: return
            dispatch(CapabilityRequestResolved(action.capability, authorization))
        } catch (cancelled: CancellationException) {
            dispatch(CapabilityRequestFailed(action.capability))
            throw cancelled
        } catch (_: Throwable) {
            dispatch(CapabilityRequestFailed(action.capability))
        }
    }
}

internal class CapabilityRequestCoordinator {
    private val mutex = Mutex()

    suspend fun <T> runExclusive(block: suspend () -> T): T? {
        if (!mutex.tryLock()) return null
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}
