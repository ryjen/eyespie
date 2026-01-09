package com.micrantha.bluebell.platform

actual class Locale {
    private val locale: java.util.Locale = java.util.Locale.getDefault()

    actual fun toLanguageTag(): String = locale.toLanguageTag()
}
