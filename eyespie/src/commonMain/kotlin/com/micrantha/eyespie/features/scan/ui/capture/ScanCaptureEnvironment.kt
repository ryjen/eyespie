package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.onboarding.data.CapabilityPermissionGateway
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import com.micrantha.eyespie.features.scan.entities.ScanAction.Back
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationLoaded
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestFailed
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestResolved
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestStarted
import com.micrantha.eyespie.features.scan.entities.ScanAction.OpenCameraSettings
import com.micrantha.eyespie.features.scan.entities.ScanAction.RefreshCameraAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanAction.RequestCameraAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanAction.ScanError
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.entities.ScanState
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import okio.Path

class ScanCaptureEnvironment(
    private val context: ScreenContext,
    private val capabilityPermissionGateway: CapabilityPermissionGateway,
) : Reducer<ScanState>, Effect<ScanState>,
    Dispatcher by context.dispatcher {
    private val cameraRequestCoordinator = ScanCameraRequestCoordinator()

    override suspend fun invoke(action: Action, state: ScanState) {
        when (action) {
            is RefreshCameraAuthorization -> refreshCameraAuthorization(state)
            is RequestCameraAuthorization -> requestCameraAuthorization(state)
            is OpenCameraSettings -> {
                if (state.cameraAuthorization == CapabilityAuthorization.SettingsRequired) {
                    capabilityPermissionGateway.openSettings(OnboardingCapability.CameraScanning)
                }
            }

            is Path -> try {
                context.navigate<ScanEditScreen, ScanEditParams>(
                    options = Router.Options.Replace,
                    arg = ScanEditParams(action, state.location!!),
                )
            } catch (_: Throwable) {
                dispatch(ScanError)
            }

            is Back -> context.router.navigateBack()
        }
    }

    override fun reduce(state: ScanState, action: Action) = when (action) {
        is Location -> state.copy(
            location = action,
        )

        is CameraAuthorizationLoaded -> if (
            !state.cameraRequestInFlight &&
            state.cameraAuthorization == action.previous
        ) {
            state.copy(
                cameraAuthorization = action.authorization,
                cameraAuthorizationLoaded = true,
            )
        } else {
            state
        }

        is CameraAuthorizationRequestStarted -> if (
            state.cameraAuthorizationLoaded &&
            !state.cameraRequestInFlight &&
            state.cameraAuthorization.canRequestCamera()
        ) {
            state.copy(cameraRequestInFlight = true)
        } else {
            state
        }

        is CameraAuthorizationRequestResolved -> if (state.cameraRequestInFlight) {
            state.copy(
                cameraAuthorization = action.authorization,
                cameraAuthorizationLoaded = true,
                cameraRequestInFlight = false,
            )
        } else {
            state
        }

        is CameraAuthorizationRequestFailed -> if (state.cameraRequestInFlight) {
            state.copy(cameraRequestInFlight = false)
        } else {
            state
        }

        is Path -> state.copy(
            busy = true,
            enabled = false,
        )

        is ScanError -> state.copy(
            enabled = true,
            busy = false,
        )

        else -> state
    }

    private suspend fun refreshCameraAuthorization(state: ScanState) {
        if (state.cameraRequestInFlight) return

        val previous = state.cameraAuthorization
        val authorization = capabilityPermissionGateway.loadCapabilities(
            previous = listOf(
                CapabilityState(
                    capability = OnboardingCapability.CameraScanning,
                    authorization = previous,
                    canRequestDuringOnboarding = true,
                ),
            ),
        ).firstOrNull { it.capability == OnboardingCapability.CameraScanning }
            ?.authorization
            ?: previous

        dispatch(
            CameraAuthorizationLoaded(
                previous = previous,
                authorization = authorization,
            ),
        )
    }

    private suspend fun requestCameraAuthorization(state: ScanState) {
        if (
            !state.cameraAuthorizationLoaded ||
            state.cameraRequestInFlight ||
            !state.cameraAuthorization.canRequestCamera()
        ) {
            return
        }

        try {
            val authorization = cameraRequestCoordinator.runExclusive {
                dispatch(CameraAuthorizationRequestStarted)
                capabilityPermissionGateway.requestAuthorization(
                    capability = OnboardingCapability.CameraScanning,
                    previous = state.cameraAuthorization,
                )
            } ?: return
            dispatch(CameraAuthorizationRequestResolved(authorization))
        } catch (cancelled: CancellationException) {
            dispatch(CameraAuthorizationRequestFailed)
            throw cancelled
        } catch (_: Throwable) {
            dispatch(CameraAuthorizationRequestFailed)
        }
    }

    private fun CapabilityAuthorization.canRequestCamera(): Boolean =
        this == CapabilityAuthorization.NotRequested || this == CapabilityAuthorization.Denied
}

internal class ScanCameraRequestCoordinator {
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
