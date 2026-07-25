package com.micrantha.eyespie

import com.micrantha.bluebell.platform.NetworkMonitor
import com.micrantha.eyespie.model.DefaultIosModelDeliveryCapabilities
import com.micrantha.eyespie.model.IosModelAssetTransport
import com.micrantha.eyespie.model.IosModelDeliveryCapabilities
import com.micrantha.eyespie.model.IosModelDeliveryCapabilitySnapshot
import com.micrantha.eyespie.model.UnconfiguredIosModelAssetTransport

class AppDelegate(
    val networkMonitor: NetworkMonitor,
    val packageId: String,
) {
    var modelDeliveryCapabilities: IosModelDeliveryCapabilities =
        DefaultIosModelDeliveryCapabilities
        private set

    var modelAssetTransport: IosModelAssetTransport = UnconfiguredIosModelAssetTransport
        private set

    /**
     * Installs the real native transport and atomically enables the corresponding capability.
     * The Kotlin-only composition path remains fail-closed because it never calls this method.
     */
    fun installModelAssetTransport(transport: IosModelAssetTransport) {
        modelAssetTransport = transport
        modelDeliveryCapabilities = IosModelDeliveryCapabilities {
            IosModelDeliveryCapabilitySnapshot(
                backgroundUrlSessionSupported = true,
                backgroundUrlSessionConfigured = true,
                backgroundAssetsSupported = false,
                backgroundAssetsConfigured = false,
            )
        }
    }
}
