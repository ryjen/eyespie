package com.micrantha.bluebell.observability.entity

sealed interface RejectionReason {
    object InvalidFormat : RejectionReason
    object TooLarge : RejectionReason
    object RateLimited : RejectionReason
    object Duplicate : RejectionReason
    data class ValidationFailed(val errors: List<String>) : RejectionReason
    data class Other(val code: String) : RejectionReason
}
