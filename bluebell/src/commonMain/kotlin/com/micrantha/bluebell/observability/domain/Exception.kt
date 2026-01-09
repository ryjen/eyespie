package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.FailureReason
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.entity.ValidationError
import com.micrantha.bluebell.observability.entity.ValidationErrorReason
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Base exception for observability errors
sealed class ObservabilityException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// Schema-related exceptions
sealed class SchemaException(
    message: String,
    cause: Throwable? = null
) : ObservabilityException(message, cause)

class SchemaValidationException(
    val errors: List<ValidationError>,
    message: String = "Schema validation failed: ${errors.joinToString { it.message }}"
) : SchemaException(message) {
    constructor(error: ValidationError) : this(listOf(error))
    constructor(message: String) : this(
        listOf(
            ValidationError(
                field = "unknown",
                reason = ValidationErrorReason.CustomValidation("general"),
                message = message
            )
        )
    )
}

class SchemaNotFoundException(
    val schemaVersion: SchemaVersion,
    message: String = "Schema not found: $schemaVersion"
) : SchemaException(message)

class SchemaAlreadyExistsException(
    val schemaVersion: SchemaVersion,
    message: String = "Schema already exists: $schemaVersion"
) : SchemaException(message)

class IncompatibleSchemaException(
    val oldVersion: SchemaVersion,
    val newVersion: SchemaVersion,
    val incompatibilities: List<String>,
    message: String = "Schema $newVersion is incompatible with $oldVersion: ${incompatibilities.joinToString()}"
) : SchemaException(message)

class MigrationNotFoundException(
    val fromVersion: SchemaVersion,
    val toVersion: SchemaVersion,
    message: String = "No migration path from $fromVersion to $toVersion"
) : SchemaException(message)

class MigrationFailedException(
    val event: TelemetryEvent,
    val targetVersion: SchemaVersion,
    message: String,
    cause: Throwable? = null
) : SchemaException(message, cause)

// Cache-related exceptions
sealed class CacheException(
    message: String,
    cause: Throwable? = null
) : ObservabilityException(message, cause)

class CacheWriteException(
    val eventId: String,
    message: String = "Failed to write event $eventId to cache",
    cause: Throwable? = null
) : CacheException(message, cause)

class CacheReadException(
    message: String = "Failed to read from cache",
    cause: Throwable? = null
) : CacheException(message, cause)

class CacheFullException(
    val currentSize: Int,
    val maxSize: Int,
    message: String = "Cache is full: $currentSize/$maxSize events"
) : CacheException(message)

// Destination-related exceptions
sealed class DestinationException(
    val destination: Destination,
    message: String,
    cause: Throwable? = null,
    val temporary: Boolean = false
) : ObservabilityException(message, cause)

class DestinationUnavailableException(
    destination: Destination,
    message: String = "$destination is unavailable",
    cause: Throwable? = null,
    temporary: Boolean = true
) : DestinationException(destination, message, cause, temporary)

class DestinationConfigurationException(
    destination: Destination,
    message: String,
    cause: Throwable? = null
) : DestinationException(destination, message, cause, temporary = false)

class DestinationRejectionException(
    destination: Destination,
    val eventId: String,
    val reason: String,
    message: String = "$destination rejected event $eventId: $reason",
    cause: Throwable? = null
) : DestinationException(destination, message, cause, temporary = false)

// Network-related exceptions
class NetworkException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null
) : ObservabilityException(message, cause)

// Rate limiting
class RateLimitException(
    val destination: Destination,
    val retryAfter: Duration? = null,
    message: String = "Rate limit exceeded for $destination" +
            (retryAfter?.let { ", retry after $it" } ?: "")
) : ObservabilityException(message)

// Event-related exceptions
class InvalidEventException(
    val eventId: String,
    message: String,
    cause: Throwable? = null
) : ObservabilityException(message, cause)

class EventTooLargeException(
    val eventId: String,
    val size: Long,
    val maxSize: Long,
    message: String = "Event $eventId is too large: $size bytes (max: $maxSize)"
) : ObservabilityException(message)

// Span-related exceptions
class SpanNotFoundException(
    val spanId: String,
    message: String = "Span not found: $spanId"
) : ObservabilityException(message)

class SpanAlreadyEndedException(
    val spanId: String,
    message: String = "Span already ended: $spanId"
) : ObservabilityException(message)

// Campaign-related exceptions
class CampaignNotFoundException(
    val campaignId: String,
    message: String = "Campaign not found: $campaignId"
) : ObservabilityException(message)

class CampaignAlreadyExistsException(
    val campaignId: String,
    message: String = "Campaign already exists: $campaignId"
) : ObservabilityException(message)

// Extension to check if exception is retryable
fun Throwable.isRetryable(): Boolean = when (this) {
    is NetworkException -> statusCode in 500..599 || statusCode == 429
    is RateLimitException -> true
    is DestinationException -> temporary
    is CacheWriteException -> true
    is CacheFullException -> false
    is SchemaValidationException -> false
    else -> false
}

// Extension to get retry delay
fun Throwable.getRetryDelay(): Duration = when (this) {
    is RateLimitException -> retryAfter ?: Duration.ZERO
    is NetworkException -> when (statusCode) {
        429 -> 60.seconds
        in 500..599 -> 30.seconds
        else -> 5.seconds
    }

    is DestinationException -> if (temporary) 10.seconds else Duration.ZERO
    else -> 5.seconds
}

// Extension to convert to FailureReason
fun Throwable.toFailureReason(): FailureReason = when (this) {
    is SchemaValidationException -> FailureReason.SchemaValidation
    is NetworkException -> FailureReason.NetworkError
    is RateLimitException -> FailureReason.RateLimitExceeded
    is DestinationException -> FailureReason.DestinationUnavailable
    is DestinationConfigurationException -> FailureReason.InvalidDestination
    else -> FailureReason.Unknown(this)
}

// Extension to get user-friendly message
fun Throwable.getUserMessage(): String = when (this) {
    is SchemaValidationException -> "Event validation failed. Please check your data format."
    is NetworkException -> "Network error occurred. Please check your connection."
    is RateLimitException -> "Too many requests. Please try again later."
    is DestinationUnavailableException -> "Service temporarily unavailable."
    is CacheFullException -> "Local storage is full. Some events may not be saved."
    else -> message ?: "An unexpected error occurred."
}
