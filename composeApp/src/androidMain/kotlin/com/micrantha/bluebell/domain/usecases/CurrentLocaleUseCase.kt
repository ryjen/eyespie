package com.micrantha.bluebell.domain.usecases

import android.content.Context
import java.util.Locale

class CurrentLocaleUseCase(
    private val context: Context
) {
    operator fun invoke(): Result<Locale> = try {
        // TODO: read from user preferences
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) throw IllegalStateException("No locales found")
        Result.success(locales[0])
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
