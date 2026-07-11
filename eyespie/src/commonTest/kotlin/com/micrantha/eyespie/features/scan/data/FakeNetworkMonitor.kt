package com.micrantha.eyespie.features.scan.data

import com.micrantha.bluebell.platform.NetworkMonitor

class FakeNetworkMonitor : NetworkMonitor {
    private var onUpdate: ((Boolean) -> Unit)? = null

    override fun startMonitoring(onUpdate: (Boolean) -> Unit) {
        this.onUpdate = onUpdate
    }

    override fun stopMonitoring() {
        onUpdate = null
    }

    fun update(connected: Boolean) {
        onUpdate?.invoke(connected)
    }
}
