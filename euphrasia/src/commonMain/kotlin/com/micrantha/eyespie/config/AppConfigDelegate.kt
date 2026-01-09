package com.micrantha.eyespie.config
import kotlin.reflect.KProperty

internal class DefaultAppConfigDelegate(
    private val defaultValue: String = ""
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String =
        EnvConfig.get(property.name) ?: defaultValue
}

internal typealias AppConfigDelegate = EnvConfig
