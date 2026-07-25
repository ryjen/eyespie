package com.micrantha.eyespie.features.onboarding.data

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CapabilityPermissionGateway {
    suspend fun loadCapabilities(previous: List<CapabilityState>): List<CapabilityState>

    suspend fun requestAuthorization(
        capability: OnboardingCapability,
        previous: CapabilityAuthorization,
    ): CapabilityAuthorization

    fun openSettings(capability: OnboardingCapability)
}

class MokoCapabilityPermissionGateway(
    private val permissionsController: PermissionsController,
) : CapabilityPermissionGateway {
    private val authorizationMutex = Mutex()

    override suspend fun loadCapabilities(
        previous: List<CapabilityState>,
    ): List<CapabilityState> = authorizationMutex.withLock {
        val cameraPrevious = previous.authorizationFor(OnboardingCapability.CameraScanning)
        val notificationsPrevious = previous.authorizationFor(OnboardingCapability.Notifications)

        listOf(
            CapabilityState(
                capability = OnboardingCapability.CameraScanning,
                authorization = safely(cameraPrevious) {
                    currentCameraAuthorization(permissionsController, cameraPrevious)
                },
                canRequestDuringOnboarding = true,
            ),
            CapabilityState(
                capability = OnboardingCapability.Notifications,
                authorization = safely(notificationsPrevious) {
                    currentNotificationAuthorization(permissionsController, notificationsPrevious)
                },
                canRequestDuringOnboarding = false,
            ),
        )
    }

    override suspend fun requestAuthorization(
        capability: OnboardingCapability,
        previous: CapabilityAuthorization,
    ): CapabilityAuthorization = authorizationMutex.withLock {
        when (capability) {
            OnboardingCapability.CameraScanning -> requestCamera(previous)
            OnboardingCapability.Notifications -> previous
        }
    }

    override fun openSettings(capability: OnboardingCapability) {
        if (capability == OnboardingCapability.CameraScanning) {
            permissionsController.openAppSettings()
        }
    }

    private suspend fun requestCamera(previous: CapabilityAuthorization): CapabilityAuthorization = try {
        permissionsController.providePermission(Permission.CAMERA)
        currentCameraAuthorization(permissionsController, previous)
    } catch (_: DeniedAlwaysException) {
        CapabilityAuthorization.SettingsRequired
    } catch (_: DeniedException) {
        CapabilityAuthorization.Denied
    } catch (_: RequestCanceledException) {
        previous
    }

    private suspend fun safely(
        fallback: CapabilityAuthorization,
        block: suspend () -> CapabilityAuthorization,
    ): CapabilityAuthorization = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        fallback
    }
}

internal expect suspend fun currentCameraAuthorization(
    permissionsController: PermissionsController,
    previous: CapabilityAuthorization,
): CapabilityAuthorization

internal expect suspend fun currentNotificationAuthorization(
    permissionsController: PermissionsController,
    previous: CapabilityAuthorization,
): CapabilityAuthorization

internal fun PermissionState.toCapabilityAuthorization(
    previous: CapabilityAuthorization = CapabilityAuthorization.NotRequested,
): CapabilityAuthorization = when (this) {
    PermissionState.NotDetermined -> CapabilityAuthorization.NotRequested
    PermissionState.NotGranted -> when (previous) {
        CapabilityAuthorization.Denied,
        CapabilityAuthorization.SettingsRequired,
        CapabilityAuthorization.Restricted -> previous

        else -> CapabilityAuthorization.NotRequested
    }

    PermissionState.Granted -> CapabilityAuthorization.Granted
    PermissionState.Denied -> CapabilityAuthorization.Denied
    PermissionState.DeniedAlways -> CapabilityAuthorization.SettingsRequired
}

private fun List<CapabilityState>.authorizationFor(
    capability: OnboardingCapability,
): CapabilityAuthorization = firstOrNull { it.capability == capability }?.authorization
    ?: CapabilityAuthorization.NotRequested
