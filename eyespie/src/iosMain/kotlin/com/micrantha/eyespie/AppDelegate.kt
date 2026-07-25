package com.micrantha.eyespie

import com.micrantha.bluebell.platform.NetworkMonitor
import com.micrantha.eyespie.model.DefaultIosModelDeliveryCapabilities
import com.micrantha.eyespie.model.IosModelDeliveryCapabilities

class AppDelegate(
    val networkMonitor: NetworkMonitor,
    val packageId: String,
) {
    var modelDeliveryCapabilities: IosModelDeliveryCapabilities =
        DefaultIosModelDeliveryCapabilities
}
