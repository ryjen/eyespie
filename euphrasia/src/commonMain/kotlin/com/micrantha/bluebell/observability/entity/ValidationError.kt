package com.micrantha.bluebell.observability.entity

data class ValidationError(
    val field: String,
    val reason: ValidationErrorReason,
    val message: String
)
