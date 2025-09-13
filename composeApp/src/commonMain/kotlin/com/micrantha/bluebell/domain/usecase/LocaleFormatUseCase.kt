package com.micrantha.bluebell.domain.usecase

import com.micrantha.bluebell.platform.Platform
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class LocaleFormatUseCase(
    private val platform: Platform,
) {
    @OptIn(ExperimentalTime::class)
    fun extended(instant: Instant): String {
        val zone = TimeZone.currentSystemDefault()

        // TODO: minimize format based upon duration to now
        return platform.format(
            instant.epochSeconds,
            DATE_TIME_LONG,
            zone.id
        )
    }

    companion object {
        const val DATE_TIME_LONG = "MMM dd yyyy 'at' hh:mm a"
    }
}
