package com.micrantha.bluebell.platform

import android.content.Context
import java.util.Locale as SystemLocale

actual class Locale(
    context: Context
) {
    val systemLocale: SystemLocale

    init {
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) throw IllegalStateException("No locales found")
        systemLocale = locales[0]
    }

    actual fun toLanguageTag(): String = systemLocale.toLanguageTag()
}
