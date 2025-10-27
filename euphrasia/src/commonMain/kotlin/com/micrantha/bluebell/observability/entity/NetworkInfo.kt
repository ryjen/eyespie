package com.micrantha.bluebell.observability.entity

data class NetworkInfo(
    val connectionType: String, // "wifi", "cellular", "ethernet", "none"
    val isMetered: Boolean,
    val effectiveConnectionType: String? = null // "slow-2g", "2g", "3g", "4g"
)
