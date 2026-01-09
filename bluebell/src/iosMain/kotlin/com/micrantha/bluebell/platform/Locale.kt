package com.micrantha.bluebell.platform

import platform.Foundation.NSLocale
import platform.Foundation.autoupdatingCurrentLocale
import platform.Foundation.languageIdentifier

actual class Locale {
    val systemLocale by lazy { NSLocale.autoupdatingCurrentLocale() }

    actual fun toLanguageTag(): String {
        return systemLocale.languageIdentifier()
    }
}
