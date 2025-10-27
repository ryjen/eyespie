package com.micrantha.bluebell.observability.usecase

import com.micrantha.bluebell.observability.debug
import com.micrantha.bluebell.observability.domain.CacheFullException
import com.micrantha.bluebell.observability.domain.NetworkException
import com.micrantha.bluebell.observability.domain.ObservabilityRepository
import com.micrantha.bluebell.observability.domain.SchemaValidationException
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.error
import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.observability.warn
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TrackUserAction(private val observability: ObservabilityRepository) {
    private val logger by logger()

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    suspend fun trackUserAction(action: String, userId: String) {
        val event = AnalyticsEvent.FeatureUsage(
            eventId = Uuid.random().toString(),
            properties = mapOf("action" to action)
        )

        observability.record(event).fold(
            onSuccess = {
                logger.debug("Event recorded successfully")
            },
            onFailure = { error ->
                when (error) {
                    is SchemaValidationException -> {
                        logger.error("Invalid event schema: ${error.errors}")
                        // Show user-friendly message
                    }

                    is NetworkException -> {
                        logger.warn("Network error, event cached for retry")
                        // Silent retry, don't bother user
                    }

                    is CacheFullException -> {
                        logger.error("Cache full, some events may be lost")
                    }

                    else -> {
                        logger.error("Unexpected error", error)
                    }
                }
            }
        )
    }
}
