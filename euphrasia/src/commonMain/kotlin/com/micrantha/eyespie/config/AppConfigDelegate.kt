package com.micrantha.eyespie.config

import kotlin.reflect.KProperty

internal class DefaultAppConfigDelegate(
    private val defaultValue: String = ""
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String =
        AppConfigDelegate.get(property.name) ?: defaultValue
}

internal object AppConfigDelegate {
    private val configMap = mapOf(
        "LOGIN_EMAIL" to EnvConfig.LOGIN_EMAIL,
        "LOGIN_PASSWORD" to EnvConfig.LOGIN_PASSWORD,
        "SUPABASE_URL" to EnvConfig.SUPABASE_URL,
        "SUPABASE_KEY" to EnvConfig.SUPABASE_KEY
    )

    fun get(key: String): String? = configMap[key]

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String =
        configMap[property.name] ?: ""
}
