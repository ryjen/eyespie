package com.micrantha.eyespie.config

import kotlin.reflect.KProperty

internal class DefaultAppConfigDelegate(
    private val defaultValue: String = ""
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String =
        AppConfigDelegate.get(property.name) ?: defaultValue
}

internal object AppConfigDelegate {
    fun get(key: String): String? = EnvConfig.get(key)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String =
        EnvConfig.get(property.name) ?: ""
}
