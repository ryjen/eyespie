package com.micrantha.eyespie.features.onboarding.data

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.test.PermissionsControllerMock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CapabilityPermissionGatewayAndroidTest {
    @Test
    fun `camera request returns reconciled authorization`() = runTest {
        var requestCount = 0
        val permissions = object : PermissionsControllerMock() {
            override suspend fun providePermission(permission: Permission) {
                requestCount += 1
            }

            override suspend fun isPermissionGranted(permission: Permission) = true

            override suspend fun getPermissionState(permission: Permission) = PermissionState.Granted

            override fun openAppSettings() = Unit
        }
        val gateway = MokoCapabilityPermissionGateway(permissions)

        val result = gateway.requestAuthorization(
            OnboardingCapability.CameraScanning,
            CapabilityAuthorization.NotRequested,
        )

        assertEquals(CapabilityAuthorization.Granted, result)
        assertEquals(1, requestCount)
    }
}
