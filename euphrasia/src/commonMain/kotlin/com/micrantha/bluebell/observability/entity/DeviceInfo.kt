package com.micrantha.bluebell.observability.entity

data class DeviceInfo(
    val deviceId: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String,
    val locale: String,
    val timezone: String
)
