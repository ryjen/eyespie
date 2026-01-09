package com.micrantha.bluebell.observability.entity

sealed interface FailureReason {
    object SchemaValidation : FailureReason
    object NetworkError : FailureReason
    object RateLimitExceeded : FailureReason
    object DestinationUnavailable : FailureReason
    object InvalidDestination : FailureReason
    data class Unknown(val exception: Throwable) : FailureReason
}
